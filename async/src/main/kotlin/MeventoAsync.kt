package com.ml.labs

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CompletionStage

class MEventoAsync(
    from: MEvento? = null,
    debug: Boolean = false,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MEvento(debug, source = from) {

    private fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> {
        return CoroutineScope(dispatcher).async(block = block)
    }

    private fun visit(node: AST): Deferred<*>? {
        val methodName = "visit${node::class.simpleName}"
        val method = this::class.java.declaredMethods.find { it.name == methodName }
            ?: throw Throwable("No $methodName declared")
        return try {
            method.invoke(this, node) as? Deferred<*>
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    override fun visitRootAST(node: AST): Deferred<Any?> {
        val list = (node as RootAST).body
        return async {
            var last: Any? = null
            for (n in list) {
                last = visit(n)?.await()
                if (last is ReturnBranch) {
                    return@async last.value
                }
            }
            last
        }
    }

    override fun visitBlockStatementAST(node: AST): Deferred<Any?> {
        val list = (node as BlockStatementAST).body
        return async {
            var last: Any? = null
            pushScope("Block")
            for (n in list) {
                last = visit(n)?.await()
                if (last is LoopControl) {
                    break
                }
                if (last is ReturnBranch) {
                    break
                }
            }
            popScope()
            last
        }
    }

    override fun visitIdentifierAST(node: AST): Deferred<Any?> {
        val id = (node as IdentifierAST).value
        return async { resolve(id) }
    }

    override fun visitLiteralAST(node: AST): Deferred<Any?> {
        return async { (node as LiteralAST).value }
    }

    override fun visitAssignmentExpressionAST(node: AST): Deferred<Any?> {
        val assignment = node as AssignmentExpressionAST
        return async {
            val id = assignment.identifier
            val init = assignment.init
            var value: Any? = null
            if (id is IndexAccessorAST) {
                val target = visit(id.owner)?.await()
                value = visit(init)?.await()
                val property = visit(id.key)?.await()
                assignProperty(target, property, value)
            } else if (id is IdentifierAST) {
                value = visit(init)?.await()
                _changeVariable(id.value, value)
            }
            value
        }
    }

    override fun visitExpressionStatementAST(node: AST): Deferred<*>? {
        return visit((node as ExpressionStatementAST).expression)
    }

    override fun visitCallExpressionAST(node: AST): Deferred<Any?> {
        val call = node as CallExpressionAST
        return async {
            val callee = call.callee as IdentifierAST
            val fn = resolveFunction(callee.value) ?: return@async null
            val argValues = call.arguments.map { visit(it) as Deferred<*> }.awaitAll()
            when (val result = fn.invoke(argValues, this@MEventoAsync)) {
                is Deferred<*> -> result.await()
                is CompletionStage<*> -> result.await()
                else -> result
            }
        }
    }

    override fun visitBinaryExpressionAST(node: AST): Deferred<Any?> {
        val binary = node as BinaryExpressionAST
        return async {
            val lValue = visit(binary.left)?.await()
            val rValue = visit(binary.right)?.await()
            val op = binary.operation
            when (op.type) {
                TokenType.plus -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue + rValue
                    }
                    return@async "$lValue$rValue"
                }

                TokenType.minus -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue - rValue
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.mult -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue * rValue
                    }
                    if (lValue is String && rValue is Number) {
                        return@async lValue.repeat(rValue.toInt())
                    }
                    if (lValue is Number && rValue is String) {
                        return@async rValue.repeat(lValue.toInt())
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.div -> {
                    if (lValue is Number && rValue is Number) {
                        if (rValue.toDouble() == 0.0) {
                            throw RuntimeException("Invalid division by 0")
                        }
                        return@async lValue / rValue
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.mod -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue % rValue
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.great -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue.toDouble() > rValue.toDouble()
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.greatEq -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue.toDouble() >= rValue.toDouble()
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.less -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue.toDouble() < rValue.toDouble()
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.lessEq -> {
                    if (lValue is Number && rValue is Number) {
                        return@async lValue.toDouble() <= rValue.toDouble()
                    }
                    throw RuntimeException("Operation ${op.value} not allowed no num value")
                }

                TokenType.eqeq -> {
                    return@async if (lValue is Number && rValue is Number) {
                        lValue.toDouble() == rValue.toDouble()
                    } else {
                        lValue == rValue
                    }
                }

                TokenType.notEq -> {
                    return@async if (lValue is Number && rValue is Number) {
                        lValue.toDouble() != rValue.toDouble()
                    } else {
                        lValue != rValue
                    }
                }

                else -> throw RuntimeException("Operation ${op.value} not allowed no num value")
            }
        }
    }

    override fun visitUnaryExpressionAST(node: AST): Deferred<Any?> {
        val unary = node as UnaryExpressionAST
        return async {
            val argValue = visit(unary.argument)?.await()
            if (unary.operation.type == TokenType.not) {
                return@async !_boolValue(argValue)
            }
            if (argValue !is Number) {
                throw RuntimeException("Operation ${unary.operation.value} not allowed no num value")
            }
            when (unary.operation.type) {
                TokenType.plus -> argValue
                TokenType.minus -> 0 - argValue
                else -> throw RuntimeException("Operation ${unary.operation.value} not allowed no num value")
            }
        }
    }

    override fun visitIfStatementAST(node: AST): Deferred<Any?> {
        val ifNode = node as IfStatementAST
        return async {
            val test = visit(ifNode.test)?.await()
            if (_boolValue(test)) {
                visit(ifNode.consequent)?.await()
            } else {
                ifNode.alternate?.let { visit(it)?.await() }
            }
        }
    }

    override fun visitLogicalExpressionAST(node: AST): Deferred<Any?> {
        val logical = node as LogicalExpressionAST
        return async {
            val test = visit(logical.left)?.await()
            if (logical.operator.type == TokenType.nullity) {
                if (test != null) {
                    return@async test
                }
                return@async visit(logical.right)?.await()
            }
            val testBoolValue = _boolValue(test)
            when (logical.operator.type) {
                TokenType.and -> if (!testBoolValue) false else _boolValue(visit(logical.right)?.await())
                TokenType.or -> if (testBoolValue) true else _boolValue(visit(logical.right)?.await())
                else -> null
            }
        }
    }

    override fun visitIndexAccessorAST(node: AST): Deferred<Any?> {
        val accessor = node as IndexAccessorAST
        return async {
            when (val owner = visit(accessor.owner)?.await()) {
                is Map<*, *> -> {
                    val key = visit(accessor.key)?.await()
                    owner[key]
                }

                is List<*> -> {
                    val key = visit(accessor.key)?.await()?.let {
                        if (it is Number) it.toInt() else it
                    }
                    if (key is Int && owner.size > key && key >= 0) {
                        owner[key]
                    } else {
                        null
                    }
                }

                else -> null
            }
        }
    }

    override fun visitObjectExpression(ast: AST): Deferred<Any?> {
        val obj = ast as ObjectExpression
        return async {
            val instance = mutableMapOf<String, Any?>()
            for (property in obj.properties) {
                if (property is ObjectProperty) {
                    val key = visit(property.key)?.await()
                    val value = visit(property.value)?.await()
                    val keyStr = key as? String ?: key.toString()
                    instance[keyStr] = value
                }
            }
            instance
        }
    }

    override fun visitArrayExpression(ast: AST): Deferred<Any?> {
        val array = ast as ArrayExpression
        return async { resolveArguments(array.elements).await().toMutableList() }
    }

    override fun visitBreakAST(node: AST): Deferred<Any?> = async { BreakBranch() }

    override fun visitWhileLoopStatement(node: AST): Deferred<Any?> {
        val whileLoop = node as WhileLoopStatement
        return async {
            val retainer = if (whileLoop.retain) mutableListOf<Any?>() else null
            while (_boolValue(visit(whileLoop.test)?.await())) {
                val ret = visit(whileLoop.body)?.await()
                when (ret) {
                    is BreakBranch -> break
                    is ContinueBranch -> continue
                    is ReturnBranch -> {
                        popScope()
                        return@async ret
                    }
                }
                retainer?.add(ret)
            }
            retainer
        }
    }

    override fun visitForLoopStatement(node: AST): Deferred<Any?> {
        val forLoop = node as ForLoopStatement
        return async {
            val retainer = if (forLoop.retain) mutableListOf<Any?>() else null
            val loopIdentifier = forLoop.init.identifier
            if (loopIdentifier !is IdentifierAST) {
                throw RuntimeException("Unexpected identifer found")
            }
            pushScope("com.ml.labs.ForLoopStatement")
            val initialValue = visit(forLoop.init.init)?.await()
            _changeVariable(loopIdentifier.value, initialValue)
            val test: suspend () -> Boolean = {
                val ret = visit(forLoop.test)?.await()
                if (ret is Number) {
                    val tmp = resolve(loopIdentifier.value)
                    if (forLoop.direction.type == TokenType.up) {
                        ret.toDouble() >= ((tmp as Number?)?.toDouble() ?: 0.0)
                    } else {
                        ret.toDouble() <= ((tmp as Number?)?.toDouble() ?: 0.0)
                    }
                } else {
                    _boolValue(ret)
                }
            }
            val update: suspend () -> Unit = {
                val updateValue = visit(forLoop.update)?.await()
                if (updateValue is Number) {
                    val tmp = resolve(loopIdentifier.value)
                    if (tmp !is Number) {
                        throw RuntimeException("Can't update value")
                    }
                    _changeVariable(
                        loopIdentifier.value,
                        if (forLoop.direction.type == TokenType.up) {
                            tmp + updateValue
                        } else {
                            tmp - updateValue
                        }
                    )
                } else {
                    throw RuntimeException("Update value can't be non-number")
                }
            }
            while (test()) {
                when (val ret = visit(forLoop.body)?.await()) {
                    is BreakBranch -> break
                    is ContinueBranch -> {
                        update()
                        continue
                    }

                    is ReturnBranch -> {
                        popScope()
                        return@async ret
                    }

                    else -> retainer?.add(ret)
                }
                update()
            }
            popScope()
            retainer
        }
    }

    override fun visitForOfStatement(node: AST): Deferred<Any?> {
        val forOf = node as ForOfStatement
        return async {
            val collection = visit(forOf.collection)?.await()
            if (collection !is List<*>) {
                throw RuntimeException("Can iterate non-array object")
            }
            val retainer = if (forOf.retain) mutableListOf<Any?>() else null
            pushScope("com.ml.labs.ForOfStatement")
            for (it in collection) {
                _declareForIdentifier(forOf.identifier, it)
                when (val ret = visit(forOf.body)?.await()) {
                    is BreakBranch -> break
                    is ContinueBranch -> continue
                    is ReturnBranch -> {
                        popScope()
                        return@async ret
                    }

                    else -> retainer?.add(ret)
                }
            }
            popScope()
            retainer
        }
    }

    override fun visitContinueAST(node: AST): Deferred<Any?> = async { ContinueBranch() }

    override fun visitReturnAST(node: AST): Deferred<Any?> {
        val ret = node as ReturnAST
        val value = ret.value
        return async { ReturnBranch(if (value != null) visit(value)?.await() else null) }
    }

    private fun resolveArguments(arguments: List<AST>): Deferred<List<Any?>> {
        return async { arguments.map { visit(it) as Deferred<*> }.awaitAll() }
    }

    override fun clone(): MEventoAsync {
        val ret = MEventoAsync(dispatcher = dispatcher)
        ret.copyAttributes(this)
        return ret
    }

    override fun execute(
        source: String,
        cache: Boolean,
        input: Map<String, Any?>?,
    ): Deferred<Any?> {
        val module = compile(source, cache)
        input?.forEach { (key, value) -> this._changeVariable(key, value) }
        return visit(module) as Deferred<Any?>
    }

    suspend fun executeSuspending(
        source: String,
        cache: Boolean = true,
        input: Map<String, Any?>? = null,
    ): Any? {
        return execute(source, cache, input).await()
    }

    fun registerSuspendFunction(
        id: String,
        fn: suspend (List<Any?>, MEventoAsync?) -> Any?,
    ) {
        registerFunction(id) { args, vm ->
            async { fn(args, vm as? MEventoAsync ?: this@MEventoAsync) }
        }
    }

    companion object {
        fun newInstance(
            from: MEvento? = null,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): MEventoAsync {
            return MEventoAsync(from, dispatcher = dispatcher)
        }

        fun run(
            source: String,
            cache: Boolean = false,
            input: Map<String, Any?>? = null,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): Deferred<Any?> {
            return MEventoAsync(dispatcher = dispatcher).execute(source, cache, input)
        }

        suspend fun runSuspending(
            source: String,
            cache: Boolean = false,
            input: Map<String, Any?>? = null,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): Any? {
            return run(source, cache, input, dispatcher).await()
        }

        fun registerSuspend(
            id: String,
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
            fn: suspend (List<Any?>, MEventoAsync?) -> Any?,
        ) {
            MEvento.register(id) { args, vm ->
                CoroutineScope(dispatcher).async { fn(args, vm as? MEventoAsync) }
            }
        }
    }
}

fun MEvento.newAsyncInstance(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): MEventoAsync {
    return MEventoAsync.newInstance(this, dispatcher)
}
