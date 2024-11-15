package com.ml.labs

import java.lang.reflect.InvocationTargetException
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue

operator fun Number.plus(other: Number): Number {
    if (other is Int) {
        return this.toInt() + other
    }
    if (other is Long) {
        return this.toLong() + other
    }
    if (other is Float) {
        return this.toFloat() + other
    }
    if (other is Double) {
        return this.toDouble() + other
    }

    return this.toLong() + other.toLong()
}

operator fun Number.minus(other: Number): Number {
    if (other is Int) {
        return this.toInt() - other
    }
    if (other is Long) {
        return this.toLong() - other
    }
    if (other is Float) {
        return this.toFloat() - other
    }
    if (other is Double) {
        return this.toDouble() - other
    }

    return this.toLong() - other.toLong()
}

operator fun Number.times(other: Number): Number {
    if (other is Int) {
        return this.toInt() * other
    }
    if (other is Long) {
        return this.toLong() * other
    }
    if (other is Float) {
        return this.toFloat() * other
    }
    if (other is Double) {
        return this.toDouble() * other
    }

    return this.toLong() * other.toLong()
}

operator fun Number.div(other: Number): Number {
    if (other is Int) {
        return this.toInt() / other
    }
    if (other is Long) {
        return this.toLong() / other
    }
    if (other is Float) {
        return this.toFloat() / other
    }
    if (other is Double) {
        return this.toDouble() / other
    }

    return this.toLong() / other.toLong()
}

operator fun Number.rem(other: Number): Number {
    if (other is Int) {
        return this.toInt() % other
    }
    if (other is Long) {
        return this.toLong() % other
    }
    if (other is Float) {
        return this.toFloat() % other
    }
    if (other is Double) {
        return this.toDouble() % other
    }

    return this.toLong() % other.toLong()
}

fun Number.eq(other: Any?): Boolean {
    if (other is Int) {
        return this.toInt() == other.toInt()
    }
    if (other is Long) {
        return this.toLong() == other.toLong()
    }
    if (other is Float) {
        return this.toFloat() == other
    }
    if (other is Double) {
        return this.toDouble() == other
    }

    return false
}

fun _boolValue(test: Any?): Boolean {
    return !(test == null ||
            (test is Number && test == 0) ||
            (test is String && test.length == 0) ||
            (test is Boolean && !test))
}

enum class TokenType {
    id, comma, semi, numberConst, stringConst, equal, lparen, rparen, eol, eof,
    lbrace, rbrace, lbracket, rbracket, great, greatEq, less, lessEq, eqeq,
    IF, ELSE, TRUE, FALSE, NULL, not, notEq, and, or, plus, minus, div, mult, mod,
    invalid, colon, WHILE_TILL, FOR_LOOP, up, down, WITH, IN, TILL, BREAK,
    CONTINUE, nullity, RETURN
}

class Token(val type: TokenType, val value: Any?, var line: Int? = null, var col: Int? = null) {
    constructor(type: TokenType, value: Any?) : this(type, value, 1, 1)

    override fun toString(): String {
        return "[${type.name}, ${value.toString()}]"
    }

    companion object {
        fun from(type: TokenType, value: Any?): Token {
            return Token(type, value)
        }
    }
}


fun error(token: Token) {
    val text = "Invalid token ${token.type.name}[${token.value}] at ${token.line}, ${token.col}\n"
    throw RuntimeException(text)
}

fun boolValue(test: Any?): Boolean {
    return (test == null ||
            (test is Number && test == 0) ||
            (test is String && test.isEmpty()) ||
            (test is Boolean && !test))
}

object Chars {
    val equal: Char get() = '='
    val comma: Char get() = ','
    val semiColon: Char get() = ';'
    val lparen: Char get() = '('
    val rparen: Char get() = ')'
    val backslash: Char get() = '\\'
    val quote: Char get() = '"'
    val squote: Char get() = '\''
    val plus: Char get() = '+'
    val minus: Char get() = '-'
    val star: Char get() = '*'
    val slash: Char get() = '/'
    val percent: Char get() = '%'
    val lbrace: Char get() = '{'
    val rbrace: Char get() = '}'
    val lbracket: Char get() = '['
    val rbracket: Char get() = ']'
    val not: Char get() = '!'
    val great: Char get() = '>'
    val less: Char get() = '<'
    val and: Char get() = '&'
    val pipe: Char get() = '|'
    val colon: Char get() = ':'
    val questionMark: Char get() = '?'
    val shebang: Char get() = '#'
}

class LexerDictionary(val lang: String, keywords: Map<String, String>) {
    val keywords = mutableMapOf<String, String>()

    init {
        this.keywords.putAll(keywords)
    }
}

abstract class AST(line: Int?, col: Int?) {
    var line: Int?
    var col: Int?

    init {
        this.line = line
        this.col = col
    }

    open fun dump(): String {
        return toString()
    }
}

data class RootAST(val body: List<AST>, val name: String, val source: String) : AST(1, 1) {
    override fun dump(): String {
        val builder = StringBuilder("Module $name Start {\n")
        for (it in body) {
            builder.append("${it.dump()}\n")
        }
        builder.append("}")
        return builder.toString()
    }
}

data class BlockStatementAST(val body: List<AST>, val token: Token) : AST(token.line, token.col) {
    override fun toString(): String {
        val statements = body.joinToString("\n") { it.toString() }
        return "{\n$statements\n}"
    }
}

data class IdentifierAST(val token: Token) : AST(token.line, token.col) {
    val value: String = token.value.toString()
    override fun toString(): String {
        return value
    }
}

data class LiteralAST(val token: Token, val raw: String) : AST(token.line, token.col) {
    val value: Any? = token.value
    override fun toString(): String {
        return value.toString()
    }
}

data class AssignmentExpressionAST(val identifier: AST, val init: AST) :
    AST(identifier.line, identifier.col) {
    override fun toString(): String {
        return "$identifier = $init"
    }
}

data class ExpressionStatementAST(val expression: AST) : AST(expression.line, expression.col) {
    override fun toString(): String {
        return expression.toString()
    }
}

data class CallExpressionAST(val callee: AST, val arguments: List<AST>) :
    AST(callee.line, callee.col) {
    override fun toString(): String {
        return "$callee(...${arguments.size})"
    }
}

data class BinaryExpressionAST(val left: AST, val operation: Token, val right: AST) :
    AST(left.line, left.col) {
    override fun toString(): String {
        return "$left $operation $right"
    }
}

data class UnaryExpressionAST(val operation: Token, val argument: AST) :
    AST(operation.line, operation.col) {
    override fun toString(): String {
        return "$operation $argument"
    }
}

data class IfStatementAST(val test: AST, val consequent: AST, val alternate: AST? = null) :
    AST(test.line, test.col) {
    override fun toString(): String {
        return "if $test $consequent ${alternate?.let { "else $it" } ?: ""}"
    }
}

data class LogicalExpressionAST(val left: AST, val operator: Token, val right: AST) :
    AST(left.line, left.col)

data class IndexAccessorAST(val owner: AST, val key: AST, val computed: Boolean = false) :
    AST(owner.line, owner.col) {
    override fun toString(): String {
        return "$owner[$key]"
    }
}

data class ObjectExpression(
    val properties: List<AST>,
    val startToken: Token?,
    val endToken: Token?,
) :
    AST(startToken?.line, startToken?.col) {
    override fun toString(): String {
        return "{...}"
    }
}

data class ArrayExpression(val elements: List<AST>, val startToken: AST?, val endToken: AST?) :
    AST(startToken?.line, startToken?.col) {
    override fun toString(): String {
        return "[...]"
    }
}

data class ObjectProperty(val key: AST, val value: AST) : AST(key.line, key.col)

data class WhileLoopStatement(
    val test: AST,
    val body: AST,
    val retain: Boolean,
    val startToken: Token?,
    val endToken: Token?,
) : AST(startToken?.line, startToken?.col)

data class ForLoopStatement(
    val init: AssignmentExpressionAST,
    val test: AST,
    val update: AST,
    val direction: Token,
    val body: AST,
    val retain: Boolean,
    val startToken: Token?,
    val endToken: Token?,
) : AST(startToken?.line, startToken?.col)

data class ForOfStatement(
    val identifier: AST,
    val collection: AST,
    val body: AST,
    val retain: Boolean,
    val startToken: Token?,
    val endToken: Token?,
) : AST(startToken?.line, startToken?.col)

data class TupleExpression(val first: AST, val second: AST) : AST(first.line, first.col)

data class BreakAST(val token: Token) : AST(token.line, token.col)

data class ReturnAST(val value: AST?, val token: Token) : AST(token.line, token.col)

data class ContinueAST(val token: Token) : AST(token.line, token.col)


class Parser(private val lexer: Lexer) {
    private val _binopPrecdences: Map<TokenType, Int> = mapOf(
        TokenType.eqeq to 10,
        TokenType.notEq to 10,
        TokenType.great to 10,
        TokenType.greatEq to 10,
        TokenType.less to 10,
        TokenType.lessEq to 10,
        TokenType.plus to 20,
        TokenType.minus to 20,
        TokenType.mult to 40,
        TokenType.div to 40,
        TokenType.mod to 40
    )
    private val _loopTrack = LinkedBlockingDeque<Boolean>()

    private var currentToken: Token? = lexer.nextToken()

    private fun _eat(type: TokenType) {
        if (currentToken?.type == type) {
            currentToken = lexer.nextToken()
        } else {
            error(currentToken!!)
        }
    }

    private fun _eatEOL() {
        while (currentToken?.type == TokenType.eol) {
            _eat(TokenType.eol)
        }
    }

    private fun _eatSemiOrEOL() {
        while (currentToken?.type == TokenType.eol || currentToken?.type == TokenType.semi) {
            _eat(currentToken!!.type)
        }
    }

    private fun _eatSemi() {
        while (currentToken?.type == TokenType.semi) {
            _eat(TokenType.semi)
        }
    }

    private fun _variable(): IdentifierAST {
        val node = IdentifierAST(currentToken!!)
        _eat(TokenType.id)
        return node
    }

    private fun _return(): ReturnAST {
        val token = currentToken
        var value: AST? = null
        if (!_expect(TokenType.eol) && !_expect(TokenType.semi)) {
            value = _expression()
        }
        return ReturnAST(value, token!!)
    }

    private fun _factor(): AST {
        val token = currentToken!!
        return when (token.type) {
            TokenType.plus, TokenType.minus, TokenType.not -> {
                _eat(currentToken!!.type)
                UnaryExpressionAST(token, _term())
            }

            TokenType.numberConst -> {
                _eat(TokenType.numberConst)
                LiteralAST(token, token.value.toString())
            }

            TokenType.stringConst -> {
                _eat(TokenType.stringConst)
                LiteralAST(token, token.value.toString())
            }

            TokenType.lparen -> {
                _eat(TokenType.lparen)
                val node = _expression()
                _eat(TokenType.rparen)
                node
            }

            TokenType.TRUE, TokenType.FALSE -> {
                _eat(token.type)
                LiteralAST(token, token.value.toString())
            }

            TokenType.NULL -> {
                _eat(TokenType.NULL)
                LiteralAST(token, "null")
            }

            TokenType.lbracket -> _arrayExpression()
            TokenType.lbrace -> _objectExpression()
            TokenType.IF -> _ifStatement()
            TokenType.WHILE_TILL -> _whileLoop(true)
            TokenType.FOR_LOOP -> _forLoop(true)
            TokenType.BREAK -> breakExpression();
            TokenType.CONTINUE -> continueExpression();
            else -> _variable()
        }
    }

    private fun breakExpression(): AST {
        if (_loopTrack.isEmpty()) {
            error(currentToken!!)
        }
        _eat(TokenType.BREAK)
        return BreakAST(currentToken!!)
    }

    private fun continueExpression(): AST {
        if (_loopTrack.isEmpty()) {
            error(currentToken!!)
        }
        _eat(TokenType.CONTINUE)
        return ContinueAST(currentToken!!)
    }

    private fun _term(): AST {
        var node = _factor()
        node = _tryParsingFunctionCall(node)
        return _tryParsingMemberExpression(node)
    }

    private fun _expression(): AST {
        var node = _term()
        node = _tryBinaryExpression(0, node)
        while (currentToken?.type in listOf(TokenType.and, TokenType.or, TokenType.nullity)) {
            val token = currentToken!!
            _eat(token.type)
            node = LogicalExpressionAST(node, token, _expression())
        }
        if (_expect(TokenType.equal)) {
            if (node is IdentifierAST || node is IndexAccessorAST) {
                val token = currentToken!!
                _eat(TokenType.equal)
                node = AssignmentExpressionAST(node, _expression())
            } else {
                throw RuntimeException("Unexpected token")
            }
        }
        return node
    }

    private fun _objectProperty(): ObjectProperty {
        val key: AST?
        when (currentToken?.type) {
            TokenType.stringConst -> {
                key = LiteralAST(currentToken!!, currentToken!!.value as String)
                _eat(TokenType.stringConst)
            }

            TokenType.lbracket -> {
                _eat(TokenType.lbracket)
                val expr = _expression()
                _eat(TokenType.rbracket)
                key = expr
            }

            TokenType.id -> {
                val id = _variable()
                key = LiteralAST(
                    Token(TokenType.id, id.value, id.line, id.col),
                    id.value
                )
            }

            else -> throw RuntimeException("Unexpected token $currentToken")
        }
        _eat(TokenType.colon)
        val value = _expression()
        return ObjectProperty(key, value)
    }

    private fun _property(): ObjectProperty {
        return _objectProperty()
    }

    private fun _objectProperties(): List<AST> {
        val properties = mutableListOf<AST>()
        if (currentToken?.type != TokenType.rbrace) {
            _eatEOL()
            properties.add(_property())
            _eatEOL()
        }
        while (currentToken?.type == TokenType.comma) {
            _eat(TokenType.comma)
            _eatEOL()
            if (_expect(TokenType.rbrace)) {
                break
            }
            properties.add(_property())
            _eatEOL()
        }
        return properties
    }

    private fun _objectExpression(braceToken: Token? = null): ObjectExpression {
        val token = braceToken ?: currentToken
        if (braceToken == null) _eat(TokenType.lbrace)
        val properties = _objectProperties()
        _eat(TokenType.rbrace)
        return ObjectExpression(properties, token, currentToken)
    }

    private fun _arrayExpression(): ArrayExpression {
        _eat(TokenType.lbracket)
        val elements = if (_expect(TokenType.rbracket)) {
            emptyList()
        } else {
            _expressionsList()
        }
        _eat(TokenType.rbracket)
        val startNode = elements.firstOrNull()
        val lastNode = elements.lastOrNull()
        return ArrayExpression(elements, startNode, lastNode)
    }

    private fun _tryParsingMemberExpression(n: AST): AST {
        var node = n
        while (currentToken!!.type == TokenType.lbracket) {
            _eat(TokenType.lbracket)
            val key = _expression()
            node = IndexAccessorAST(node, key, computed = true)
            _eat(TokenType.rbracket)
        }
        return node
    }

    private fun _tryBinaryExpression(prec: Int, left: AST): AST {
        var node = left
        while (true) {
            val currentPrec = _binopPrecdences[currentToken!!.type] ?: -1
            if (currentPrec < prec) return node
            val operator = currentToken!!
            _eat(operator.type)
            var right = _term()
            val nextPrec = _binopPrecdences[currentToken!!.type] ?: -1
            if (currentPrec < nextPrec) {
                val tmp = _tryBinaryExpression(currentPrec + 1, right)
                if (tmp == node) return tmp
                right = tmp
            }
            node = BinaryExpressionAST(node, operator, right)
        }
    }

    private fun _expressionsList(): List<AST> {
        _eatEOL()
        val expr = _expression()
        _eatEOL()
        val result = mutableListOf(expr)
        while (currentToken?.type == TokenType.comma) {
            _eat(TokenType.comma)
            _eatEOL()
            if (_expect(TokenType.rbracket)) {
                break
            }
            val nextExpr = _expression()
            result.add(nextExpr)
            _eatEOL()
        }
        return result
    }

    private fun _callExpression(node: AST): CallExpressionAST {
        _eat(TokenType.lparen)
        val arguments = if (!_expect(TokenType.rparen)) {
            _expressionsList()
        } else {
            emptyList()
        }
        _eat(TokenType.rparen)
        if (node !is IdentifierAST) {
            error(currentToken!!)
        }
        return CallExpressionAST(node, arguments)
    }

    private fun _tryParsingFunctionCall(n: AST): AST {
        var node = n
        while (currentToken!!.type == TokenType.lparen) {
            node = _callExpression(node)
        }
        return node
    }

    private fun _statementExpression(): AST {
        val node = _expression()
        if (currentToken?.type !in listOf(TokenType.semi, TokenType.eol, TokenType.eof)) {
            error(currentToken!!)
        }
        return node
    }

    private fun _blockStatement(ignoreFirstBrace: Boolean = false): BlockStatementAST {
        if (!ignoreFirstBrace) _eat(TokenType.lbrace)
        _eatEOL()
        if (_expect(TokenType.rbrace)) {
            _eat(TokenType.rbrace)
            return BlockStatementAST(emptyList(), currentToken!!)
        }
        val body = mutableListOf(_statement())
        while (true) {
            _eatSemiOrEOL()
            if (currentToken?.type == TokenType.rbrace || currentToken?.type == TokenType.eof) {
                break
            }
            body.add(_statement())
            if (_expect(TokenType.rbrace)) break
            if (currentToken?.type != TokenType.eol && currentToken?.type != TokenType.semi && currentToken?.type != TokenType.eof) {
                error(currentToken!!)
            }
        }
        _eat(TokenType.rbrace)
        if (currentToken?.type == TokenType.rbrace) {
            _eat(TokenType.rbrace)
        }
        return BlockStatementAST(body, currentToken!!)
    }

    private fun _ifStatement(): IfStatementAST {
        _eat(TokenType.IF)
        val withPar = currentToken?.type == TokenType.lparen
        if (withPar) {
            _eat(TokenType.lparen)
        }
        val test = _expression()
        if (withPar) {
            _eat(TokenType.rparen)
        }
        val consequent: AST = if (currentToken?.type == TokenType.lbrace) {
            _blockStatement()
        } else {
            _expression()
        }
        var alternate: AST? = null
        if (currentToken?.type == TokenType.ELSE) {
            _eat(TokenType.ELSE)
            alternate = when (currentToken?.type) {
                TokenType.IF -> _ifStatement()
                TokenType.lbrace -> _blockStatement()
                else -> _expression()
            }
        }
        return IfStatementAST(test, consequent, alternate)
    }

    private fun pushLoop() {
        _loopTrack.push(true)
    }

    private fun popLoop() {
        _loopTrack.pop()
    }

    private fun _whileLoop(retain: Boolean = false): WhileLoopStatement {
        val token = currentToken!!
        _eat(TokenType.WHILE_TILL)
        pushLoop()
        val test = _expression()
        val body = if (currentToken?.type == TokenType.lbrace) {
            _blockStatement()
        } else {
            _expression()
        }
        popLoop()
        return WhileLoopStatement(test, body, retain, token, currentToken!!)
    }

    private fun _forOfIdentifier(): AST {
        return when (currentToken?.type) {
            TokenType.lparen -> {
                _eat(TokenType.lparen)
                val first = _variable()
                _eat(TokenType.comma)
                val second = _variable()
                _eat(TokenType.rparen)
                TupleExpression(first, second)
            }

            else -> _variable()
        }
    }

    private fun _forLoop(retain: Boolean = false): AST {
        val startToken = currentToken!!
        _eat(TokenType.FOR_LOOP)
        pushLoop()
        var forOf = currentToken?.type == TokenType.lparen
        var node: AST
        node = if (forOf) {
            _forOfIdentifier()
        } else {
            val expr = _expression()
            if (expr !is AssignmentExpressionAST) {
                forOf = true
            }
            expr
        }
        val direction: Token
        if (!forOf && node is AssignmentExpressionAST) {
            _eat(TokenType.TILL)
            val test = _expression()
            direction =
                if (currentToken?.type == TokenType.up || currentToken?.type == TokenType.down) {
                    val t = currentToken!!
                    _eat(t.type)
                    t
                } else {
                    Token(TokenType.up, "up", currentToken!!.line, currentToken!!.col)
                }
            val update: AST = if (_expect(TokenType.WITH)) {
                _eat(TokenType.WITH)
                _expression()
            } else {
                LiteralAST(
                    Token(TokenType.numberConst, 1L, currentToken!!.line, currentToken!!.col),
                    "1"
                )
            }
            val body: AST = if (currentToken?.type == TokenType.lbrace) {
                _blockStatement()
            } else {
                _expression()
            }
            node = ForLoopStatement(
                node,
                test,
                update,
                direction,
                body,
                retain,
                startToken,
                currentToken!!
            )
            popLoop()
        } else if (forOf) {
            _eat(TokenType.IN)
            val collection = _expression()
            val body: AST
            body = if (currentToken?.type == TokenType.lbrace) {
                _blockStatement()
            } else {
                _expression()
            }
            node = ForOfStatement(node, collection, body, true, startToken, currentToken!!)
            popLoop()
        } else {
            error(currentToken!!)
        }
        return node
    }

    private fun _statement(): AST {
        return when (currentToken?.type) {
            TokenType.BREAK -> breakExpression()

            TokenType.CONTINUE -> continueExpression()

            TokenType.RETURN -> {
                _eat(TokenType.RETURN)
                _return()
            }

            TokenType.semi -> {
                _eatSemi()
                _statement()
            }

            TokenType.eol -> {
                _eatEOL()
                _statement()
            }

            TokenType.WHILE_TILL -> _whileLoop()
            TokenType.FOR_LOOP -> _forLoop()
            else -> _statementExpression()
        }
    }

    private fun _expect(type: TokenType) = currentToken?.type == type

    private fun _definition(): List<AST> {
        _eatEOL()
        if (_expect(TokenType.eof)) {
            return emptyList()
        }
        val results = mutableListOf(_statement())
        while (true) {
            _eatSemiOrEOL()
            if (currentToken?.type == TokenType.eof) {
                _eat(TokenType.eof)
                break
            }
            results.add(_statement())
        }
        return results
    }

    private fun _root(): RootAST {
        val source = lexer.source
        val name = "<module>"
        val list = _definition()
        return RootAST(list, name, source)
    }

    fun parse(): AST {
        return _root()
    }
}

class Lexer(val source: String) {
    companion object {
        val _defaultLanguage = LexerDictionary(
            "en", mapOf(
                "if" to "if",
                "else" to "else",
                "true" to "true",
                "false" to "false",
                "null" to "null",
                "while" to "while",
                "for" to "for",
                "with" to "with",
                "up" to "up",
                "down" to "down",
                "till" to "till",
                "in" to "in",
                "break" to "break",
                "continue" to "continue",
                "return" to "return"
            )
        )

        val languages = listOf(
            _defaultLanguage,
            LexerDictionary(
                "fr", mapOf(
                    "si" to "if",
                    "sinon" to "else",
                    "vrai" to "true",
                    "faux" to "false",
                    "nul" to "null",
                    "tanque" to "while",
                    "pour" to "for",
                    "avec" to "with",
                    "mont" to "up",
                    "desc" to "down",
                    "jusqua" to "till",
                    "dans" to "in",
                    "couper" to "break",
                    "continuer" to "continue",
                    "returner" to "return"
                )
            ),
            LexerDictionary(
                "bm", mapOf(
                    "nii" to "if",
                    "note" to "else",
                    "tien" to "true",
                    "galon" to "false",
                    "gansan" to "null",
                    "foo" to "while",
                    "seginka" to "for",
                    "niin" to "with",
                    "kay" to "up",
                    "kaj" to "down",
                    "kata" to "till",
                    "kono" to "in",
                    "tike" to "break",
                    "ipan" to "continue",
                    "segin" to "return"
                )
            )
        )

        val RESERVED = mapOf(
            "if" to Token.from(TokenType.IF, "if"),
            "else" to Token.from(TokenType.ELSE, "else"),
            "true" to Token.from(TokenType.TRUE, true),
            "false" to Token.from(TokenType.FALSE, false),
            "null" to Token.from(TokenType.NULL, null),
            "for" to Token.from(TokenType.FOR_LOOP, "for"),
            "while" to Token.from(TokenType.WHILE_TILL, "while"),
            "with" to Token.from(TokenType.WITH, "with"),
            "up" to Token.from(TokenType.up, "up"),
            "down" to Token.from(TokenType.down, "down"),
            "till" to Token.from(TokenType.TILL, "till"),
            "in" to Token.from(TokenType.IN, "in"),
            "break" to Token.from(TokenType.BREAK, "break"),
            "continue" to Token.from(TokenType.CONTINUE, "continue"),
            "return" to Token.from(TokenType.RETURN, "return")
        )

    }


    private var _language: LexerDictionary? = null
    private var _runes: CharArray = source.toCharArray()
    private var _position = 0
    private var _line = 1
    private var _col = 1
    private var _currentChar: Char = Char.MIN_VALUE

    init {
        _currentChar = _runes.elementAt(_position)
        resolveLanguage()
    }

    private fun resolveLanguage() {
        var token = nextToken()
        if (token.type == TokenType.less) {
            // language setting
            val idToken = nextToken()
            if (idToken.type != TokenType.id) {
                error(token)
            }
            val lang = idToken.value.toString()
            _language = languages.firstOrNull { it.lang == lang } ?: _defaultLanguage
            token = nextToken()
            if (token.type != TokenType.great) {
                error(token)
            }
        } else {
            _language = _defaultLanguage
            // reset reading
            _position = 0
            _currentChar = _runes.elementAt(_position)
        }
    }

    private fun advance() {
        _position++
        if (_position >= _runes.size) {
            _currentChar = Char.MIN_VALUE
            return
        }
        _currentChar = _runes[_position]
        _col++
    }

    private fun _pick(): Char {
        if (_position + 1 >= _runes.size) {
            return Char.MIN_VALUE
        }
        return _runes[_position + 1]
    }

    fun _jump(n: Int) {
        _position += n
        if (_position >= _runes.size) {
            _currentChar = Char.MIN_VALUE
            return
        }
        _currentChar = _runes.elementAt(_position)
        _col += n
    }

    private fun _isId(code: Int): Boolean {
        if (code < 48) {
            return code == 36
        }
        if (code < 58) {
            return true
        }
        if (code < 65) {
            return false
        }
        if (code < 91) {
            return true
        }
        if (code < 97) {
            return code == 95
        }
        if (code < 123) {
            return true
        }
        return false
    }

    private fun _isIdStart(code: Int): Boolean {
        if (code < 65) {
            return code == 36
        }
        if (code < 91) {
            return true
        }
        if (code < 97) {
            return code == 95
        }
        if (code < 123) {
            return true
        }
        return false
    }

    private fun _id(): Token {
        var ret = ""
        val lastC = _col
        val lastP = _position
        val line = _line
        while (_isId(_currentChar.code)) {
            ret += _currentChar.toChar()
            advance()
        }
        val translatedId = if (_language != null && _language!!.keywords.containsKey(ret))
            _language!!.keywords[ret]
        else
            ret
        return if (translatedId != null && RESERVED.containsKey(translatedId))
            RESERVED[translatedId]!!
        else
            Token(TokenType.id, ret, line, lastC)
    }

    private fun _isLineEnd(c: Int): Boolean {
        return c != -1 && (c == 10 || c == 13 || listOf(
            '\n',
            '\r',
            '\u2028',
            '\u2029'
        ).contains(c.toChar()))
    }

    private fun _isWhiteSpace(c: Int): Boolean {
        return c >= 0 && listOf(' ', '\t').contains(c.toChar())
    }

    private fun _isDigit(c: Int): Boolean = c >= 0 && (c xor 0x30) <= 9

    private fun _skipWhiteSpace() {
        while (_isWhiteSpace(_currentChar.code)) {
            advance()
        }
    }

    private fun _number(): Token {
        var ret = ""
        val lastC = _col
        val lastP = _position
        val line = _line
        val first = _currentChar
        advance()
        val bc = if (_currentChar.code < 0) "" else _currentChar.toString()
        val base = if (first == '0' && listOf("b", "B", "x", "X", "o", "O").contains(bc)) {
            advance()
            when (bc.lowercase()) {
                "b" -> 2
                "o" -> 8
                "x" -> 16
                else -> 10
            }
        } else {
            ret += first
            10
        }
        while (_isDigit(_currentChar.code) || (base == 16 && listOf(
                'A',
                'a',
                'B',
                'b',
                'C',
                'c',
                'D',
                'd',
                'E',
                'e',
                'F',
                'f'
            ).contains(_currentChar))
        ) {
            ret += _currentChar.toChar()
            advance()
        }
        if (_currentChar == '.' && _isDigit(_pick().code)) {
            if (base != 10) {
                error(Token(TokenType.id, bc.toString(), line, lastP))
            }
            ret += _currentChar
            advance()
            var validFloat = false
            while (_isDigit(_currentChar.code)) {
                ret += _currentChar
                validFloat = true
                advance()
            }
            if (!validFloat) {
                return Token.from(TokenType.invalid, "Invalid floating number")
            }
            return Token(TokenType.numberConst, ret.toDouble(), line, lastC)
        }
        return Token(TokenType.numberConst, ret.toLong(base), line, lastC)
    }


    fun _literalString(startChar: Char): Token {
        var ret = ""
        var lastChar = Char.MIN_VALUE
        val lastC = _col
        val lastL = _line
        while (_currentChar != Char.MIN_VALUE) {
            val p = _pick()
            val next = if (p > Char.MIN_VALUE) p else Char.MIN_VALUE
            if (_currentChar == Chars.backslash) {
                // escape char
                when (next) {
                    '\\' -> ret += "\\"
                    '0' -> ret += "\\0"
                    'a' -> ret += "\\a"
                    'b' -> ret += "\b"
                    'f' -> ret += "\u000c"
                    'n' -> ret += '\n'
                    'r' -> ret += '\r'
                    't' -> ret += '\t'
                    'u' -> {
                        ret += Char(source.substring(_position + 2, _position + 6).toInt(16))
                        _jump(4);
                    }

                    'v' -> ret += "\\v"
                    'x' -> {
                        ret += Char(source.substring(_position + 2, _position + 4).toInt(16))
                        _jump(2);
                    }

                    else -> {
                        if (startChar == next) {
                            ret += startChar
                        } else {
                            advance();
                            lastChar = _currentChar
                            ret += _currentChar
                            advance()
                            continue
                        }
                    }
                }
                // scape char

                _jump(2)
                lastChar = _currentChar
                continue
            }
            if (_currentChar == startChar && lastChar != Chars.backslash) {
                // end
                break
            }

            ret += _currentChar
            lastChar = _currentChar
            advance()
        }
        return Token(TokenType.stringConst, ret, lastL, lastC)
    }

    fun _skipLineComment() {
        while (!_isLineEnd(_currentChar.code) && _currentChar != Char.MIN_VALUE) {
            advance()
        }
    }

    fun _skipComment() {
        while (_currentChar != Char.MIN_VALUE) {
            if (_currentChar == Chars.star && _pick() == Chars.shebang) {
                // end of block comment
                advance()
                advance()
                break
            }
            advance()
        }
    }


    fun nextToken(): Token {
        val lastL = _line
        val lastC = _col
        val lastP = _position
        while (_currentChar != Char.MIN_VALUE) {
            if (_isLineEnd(_currentChar.code)) {
                _line++
                _col = 1
                advance()
                return Token(TokenType.eol, "\n", lastL, lastC)
            }

            if (_isWhiteSpace(_currentChar.code)) {
                _skipWhiteSpace()
                continue
            }
            if (this._currentChar == Chars.shebang) {
                this.advance()
                if (this._currentChar == Chars.star) {
                    // multi block comment
                    this.advance()
                    this._skipComment()
                } else {
                    this._skipLineComment()
                }
                continue
            }
            if (_isDigit(_currentChar.code)) {
                return _number()
            }
            if (_isIdStart(_currentChar.code)) {
                return _id()
            }
            if (_currentChar == Chars.equal) {
                advance()
                if (_currentChar == Chars.equal) {
                    advance()
                    return Token(TokenType.eqeq, "==", lastL, lastC)
                }
                return Token(TokenType.equal, "=", lastL, lastC)
            }
            if (_currentChar == Chars.great) {
                advance()
                if (_currentChar == Chars.equal) {
                    advance()
                    return Token(TokenType.greatEq, ">=", lastL, lastC)
                }
                return Token(TokenType.great, ">", lastL, lastC)
            }
            if (_currentChar == Chars.less) {
                advance()
                if (_currentChar == Chars.equal) {
                    advance()
                    return Token(TokenType.lessEq, "<=", lastL, lastC)
                }
                return Token(TokenType.less, "<", lastL, lastC)
            }
            if (_currentChar == Chars.semiColon) {
                advance()
                return Token(TokenType.semi, ";", lastL, lastC)
            }
            if (_currentChar == Chars.lparen) {
                advance()
                return Token(TokenType.lparen, "(", lastL, lastC)
            }
            if (_currentChar == Chars.rparen) {
                advance()
                return Token(TokenType.rparen, ")", lastL, lastC)
            }
            if (_currentChar == Chars.comma) {
                advance()
                return Token(TokenType.comma, ",", lastL, lastC)
            }
            if (_currentChar == Chars.lbrace) {
                advance()
                return Token(TokenType.lbrace, "{", lastL, lastC)
            }
            if (_currentChar == Chars.rbrace) {
                advance()
                return Token(TokenType.rbrace, "}", lastL, lastC)
            }
            if (_currentChar == Chars.lbracket) {
                advance()
                return Token(TokenType.lbracket, "[", lastL, lastC)
            }
            if (_currentChar == Chars.rbracket) {
                advance()
                return Token(TokenType.rbracket, "]", lastL, lastC)
            }
            if (_currentChar == Chars.plus) {
                advance()
                return Token(TokenType.plus, "+", lastL, lastC)
            }
            if (_currentChar == Chars.minus) {
                advance()
                return Token(TokenType.minus, "-", lastL, lastC)
            }
            if (_currentChar == Chars.slash) {
                advance()
                return Token(TokenType.div, "/", lastL, lastC)
            }
            if (_currentChar == Chars.star) {
                advance()
                return Token(TokenType.mult, "*", lastL, lastC)
            }
            if (_currentChar == Chars.percent) {
                advance()
                return Token(TokenType.mod, "%", lastL, lastC)
            }
            if (_currentChar == Chars.colon) {
                advance()
                return Token(TokenType.colon, ":", lastL, lastC)
            }
            if (_currentChar == Chars.not) {
                advance()
                if (_currentChar == Chars.equal) {
                    advance()
                    return Token(TokenType.notEq, "!=", lastL, lastC)
                }
                return Token(TokenType.not, "!", lastL, lastC)
            }
            if (_currentChar == Chars.and && _pick() == Chars.and) {
                advance()
                advance()
                return Token(TokenType.and, "&&", lastL, lastC)
            }
            if (_currentChar == Chars.pipe && _pick() == Chars.pipe) {
                advance()
                advance()
                return Token(TokenType.or, "||", lastL, lastC)
            }
            if (this._currentChar == Chars.questionMark && this._pick() == Chars.questionMark) {
                this.advance()
                this.advance()
                return Token(TokenType.nullity, "??", lastL, lastC)
            }
            if (_currentChar == Chars.quote || _currentChar == Chars.squote) {
                val startChar = _currentChar
                advance()
                val t = _literalString(startChar)
                advance()
                return t
            }
            return Token(TokenType.invalid, _currentChar.toChar().toString(), lastL, lastC)
        }
        return Token(TokenType.eof, "", lastL, lastC)
    }

}

open class LoopControl

class BreakBranch : LoopControl()

class ContinueBranch : LoopControl()

class ReturnBranch(val value: Any?)

class MEventScope(
    val name: String,
    val memory: MutableMap<String, Any?>,
    val parent: MEventScope? = null,
) {
    fun resolve(key: String): Any? {
        return memory[key] ?: parent?.resolve(key)
    }

    fun change(key: String, value: Any?, declare: Boolean = true): Boolean {
        synchronized(memory) {
            if (memory.containsKey(key)) {
                memory[key] = value
                return true
            }
            if (parent?.change(key, value, false) == true) {
                return true
            }
            if (!declare) {
                return false
            }
            memory[key] = value
            return true
        }
    }
}

// com.ml.labs.AST Wallker


open class MEvento(
    private val debug: Boolean = false,
    source: MEvento? = null,
) {

    internal val rootScope: MEventScope
    private var currentScope: MEventScope? = null
    internal val functionsRegistry: MutableMap<String, (List<Any?>, MEvento?) -> Any?>

    init {
        this.rootScope = source?.rootScope ?: MEventScope("com.ml.labs.MEvento", mutableMapOf())
        this.functionsRegistry = source?.functionsRegistry ?: mutableMapOf()
        this.functionsRegistry.putAll(globalFunctionsRegistry)
        currentScope = this.rootScope
    }

    protected fun log(message: Any) {
        if (debug) {
            println(message)
        }
    }

    protected fun resolve(name: String): Any? {
        return currentScope?.resolve(name)
    }

    fun resolveFunction(name: String): ((List<Any?>, MEvento?) -> Any?)? {
        return functionsRegistry[name]
    }

    fun copyAttributes(other: MEvento) {
        functionsRegistry.putAll(other.functionsRegistry)
        rootScope.memory.putAll(other.rootScope.memory)
    }

    protected fun _changeVariable(name: String, value: Any?): Any? {
        return if (currentScope?.change(name, value) == true) {
            value
        } else {
            null
        }
    }

    protected fun pushScope(name: String) {
        val scope = MEventScope(name, mutableMapOf(), parent = currentScope)
        currentScope = scope
    }

    protected fun popScope() {
        currentScope = currentScope?.parent
    }

    val memory: Map<String, Any?>
        get() = rootScope.memory

    fun assignArray(owner: MutableList<Any?>, property: Any?, value: Any?) {
        owner[(property as Number).toInt()] = value
    }

    fun assignMap(owner: MutableMap<String, Any?>, property: Any?, value: Any?) {
        owner[property.toString()] = value
    }

    protected fun _declareForIdentifier(node: AST, value: Any?) {
        when (node) {
            is TupleExpression -> {
                if (value !is List<*>) {
                    throw RuntimeException("Unable to make a tuple from non-array element")
                }
                _changeVariable((node.first as IdentifierAST).value, value[0])
                _changeVariable((node.second as IdentifierAST).value, value[1])
            }

            is IdentifierAST -> _changeVariable(node.value, value)
        }
    }


    fun registerFunction(id: String, fn: (List<Any?>, MEvento?) -> Any?) {
        functionsRegistry[id] = fn
    }

    fun unregisterFunction(id: String) {
        functionsRegistry.remove(id)
    }

    private fun visit(node: AST): Any? {
        val methodName = "visit${node::class.simpleName}"
        val method = this::class.java.declaredMethods.find { it.name == methodName }
            ?: throw Throwable("No $methodName declared")
        return try {
            method.invoke(this, node)
        } catch (e: InvocationTargetException) {
            null
        }
    }

    open fun visitRootAST(node: AST): Any? {
        val list = (node as RootAST).body
        var last: Any? = null
        for (n in list) {
            last = visit(n)
            if (last is ReturnBranch) {
                return last.value
            }
        }
        return last
    }

    open fun visitBlockStatementAST(node: AST): Any? {
        val list = (node as BlockStatementAST).body
        var last: Any? = null
        pushScope("Block")
        for (n in list) {
            last = visit(n)
            if (last is LoopControl) {
                break
            }
            if (last is ReturnBranch) {
                break
            }
        }
        popScope()
        return last
    }

    open fun visitIdentifierAST(node: AST): Any? {
        val id = (node as IdentifierAST).value
        return resolve(id)
    }

    open fun visitLiteralAST(node: AST): Any? {
        return (node as LiteralAST).value
    }

    open fun visitAssignmentExpressionAST(node: AST): Any? {
        val id = (node as AssignmentExpressionAST).identifier
        val init = node.init
        var value: Any? = null
        if (id is IndexAccessorAST) {
            val target = visit(id.owner)
            value = visit(node.init)
            val property = visit(id.key)
            assignProperty(target, property, value)
        } else if (id is IdentifierAST) {
            value = visit(init)
            _changeVariable(id.value, value)
        }
        return value
    }

    protected fun assignProperty(owner: Any?, property: Any?, value: Any?) {
        if (owner is MutableList<*>) {
            assignArray(owner as MutableList<Any?>, property, value)
        } else if (owner is MutableMap<*, *>) {
            assignMap(owner as MutableMap<String, Any?>, property, value)
        }
    }

    open fun visitExpressionStatementAST(node: AST): Any? {
        val expr = (node as ExpressionStatementAST).expression
        return visit(expr)
    }

    open fun visitCallExpressionAST(node: AST): Any? {
        val callee = (node as CallExpressionAST).callee as IdentifierAST
        val args = node.arguments
        val calleeName = callee.value
        val fn = functionsRegistry[calleeName]
        if (fn == null) {
            return null
        }
        val argValues = args.map { visit(it) }.toList()
        val result = fn.invoke(argValues, this)
        return result
    }


    open fun visitBinaryExpressionAST(node: AST): Any? {
        val left = (node as BinaryExpressionAST).left
        val right = node.right
        val op = node.operation
        val lValue = visit(left)
        val rValue = visit(right)
        when (op.type) {
            TokenType.plus -> {
                if (lValue is Number && rValue is Number) {
                    return lValue + rValue
                }
                return "$lValue$rValue"
            }

            TokenType.minus -> {
                if (lValue is Number && rValue is Number) {
                    return lValue - rValue
                }
                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.mult -> {
                if (lValue is Number && rValue is Number) {
                    return lValue * rValue
                }
                if (lValue is String && rValue is Number) {
                    return lValue.repeat(rValue.toInt())
                }
                if (lValue is Number && rValue is String) {
                    return rValue.repeat(lValue.toInt())
                }
                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.div -> {
                if (lValue is Number && rValue is Number) {
                    if (rValue == 0.0) {
                        throw RuntimeException("Invalid division by 0")
                    }
                    return lValue / rValue
                }
                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.mod -> {
                if (lValue is Number && rValue is Number) {
                    return lValue % rValue
                }
                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.great -> {
                if (lValue is Number && rValue is Number) {
                    return lValue.toDouble() > rValue.toDouble()
                }
                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.greatEq -> {
                if (lValue is Number && rValue is Number) {
                    return lValue.toDouble() >= rValue.toDouble()
                }
                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.less -> {
                if (lValue is Number && rValue is Number) {
                    return lValue.toDouble() < rValue.toDouble()
                }
                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.lessEq -> {
                if (lValue is Number && rValue is Number) {
                    return lValue.toDouble() <= rValue.toDouble()
                }

                throw RuntimeException("Operation ${op.value} not allowed no num value")
            }

            TokenType.eqeq -> {

                return if (lValue is Number && rValue is Number) lValue.toDouble() == rValue.toDouble() else lValue == rValue
            }

            TokenType.notEq -> {
                return if (lValue is Number && rValue is Number) lValue.toDouble() != rValue.toDouble() else lValue != rValue
            }

            else -> throw RuntimeException("Operation ${op.value} not allowed no num value")
        }
    }

    open fun visitUnaryExpressionAST(node: AST): Any? {
        val arg = (node as UnaryExpressionAST).argument
        val op = node.operation
        val argValue = visit(arg)
        if (op.type == TokenType.not) {
            return !_boolValue(argValue)
        }
        if (argValue !is Number) {
            throw RuntimeException("Operation ${op.value} not allowed no num value")
        }
        return when (op.type) {
            TokenType.plus -> argValue
            TokenType.minus -> 0 - argValue
            else -> throw RuntimeException("Operation ${op.value} not allowed no num value")
        }
    }

    open fun visitIfStatementAST(node: AST): Any? {
        val testNode = (node as IfStatementAST).test
        val test = visit(testNode)
        val testBoolValue = _boolValue(test)

        return if (testBoolValue) {
            visit(node.consequent)
        } else {
            node.alternate?.let { visit(it) }
        }
    }

    open fun visitLogicalExpressionAST(node: AST): Any? {
        val leftNode = (node as LogicalExpressionAST).left
        val test = visit(leftNode)
        if (node.operator.type == TokenType.nullity) {
            if (test != null) {
                return test
            }

            val ret = visit(node.right)
            return ret
        }
        val testBoolValue = _boolValue(test)
        return when (node.operator.type) {
            TokenType.and -> if (!testBoolValue) false else _boolValue(visit(node.right))
            TokenType.or -> if (testBoolValue) true else _boolValue(visit(node.right))
            else -> null
        }
    }

    open fun visitIndexAccessorAST(node: AST): Any? {
        val ownerNode = (node as IndexAccessorAST).owner
        return when (val owner = visit(ownerNode)) {
            is Map<*, *> -> {
                val key = visit(node.key)
                owner[key]
            }

            is List<*> -> {
                val key = visit(node.key)?.let {
                    if (it is Number) {
                        it.toInt()
                    } else {
                        it
                    }
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


    open fun visitObjectExpression(ast: AST): Any {
        val instance = mutableMapOf<String, Any?>()
        val node = ast as ObjectExpression
        for (it in node.properties) {
            if (it is ObjectProperty) {
                val key = visit(it.key)
                val value = visit(it.value)
                val keyStr = key as? String ?: key.toString()
                instance[keyStr] = value
            }
        }
        return instance
    }

    open fun visitArrayExpression(ast: AST): Any {
        val node = ast as ArrayExpression
        val args = resolveArguments(node.elements)
        return args
    }

    open fun visitBreakAST(node: AST): Any? = BreakBranch()

    open fun visitWhileLoopStatement(node: AST): Any? {
        val whileLoopNode = node as WhileLoopStatement
        val retainer = if (whileLoopNode.retain) mutableListOf<Any?>() else null
        while (_boolValue(visit(whileLoopNode.test))) {
            val ret = visit(whileLoopNode.body)
            when (ret) {
                is BreakBranch -> break
                is ContinueBranch -> continue
                is ReturnBranch -> {
                    this.popScope()
                    return ret
                }
            }
            retainer?.add(ret)
        }
        return retainer
    }

    open fun visitForLoopStatement(node: AST): Any? {
        val forLoopNode = node as ForLoopStatement
        val retainer = if (forLoopNode.retain) mutableListOf<Any?>() else null
        val loopIdentifier = forLoopNode.init.identifier
        if (loopIdentifier !is IdentifierAST) throw RuntimeException("Unexpected identifer found")
        pushScope("com.ml.labs.ForLoopStatement")
        val initialValue = visit(forLoopNode.init.init)
        _changeVariable(loopIdentifier.value, initialValue)
        // retainer?.add(initialValue)
        val test: () -> Boolean = {
            val ret = visit(forLoopNode.test)
            if (ret is Number) {
                val tmp = resolve(loopIdentifier.value)
                if (forLoopNode.direction.type == TokenType.up) {
                    ret.toDouble() >= ((tmp as Number?)?.toDouble() ?: 0.0)
                } else {
                    ret.toDouble() <= ((tmp as Number?)?.toDouble() ?: 0.0)
                }
            } else {
                _boolValue(ret)
            }
        }
        val update: () -> Unit = {
            val updateValue = visit(forLoopNode.update)
            if (updateValue is Number) {
                val tmp = resolve(loopIdentifier.value)
                if (tmp !is Number) throw RuntimeException("Can't update value")
                _changeVariable(
                    loopIdentifier.value,
                    if (forLoopNode.direction.type == TokenType.up) {
                        (tmp + updateValue)
                    } else {
                        (tmp - updateValue)
                    }
                )
            } else {
                throw RuntimeException("Update value can't be non-number")
            }
        }
        while (test()) {
            when (val ret = visit(forLoopNode.body)) {
                is BreakBranch -> break
                is ContinueBranch -> {
                    update()
                    continue
                }

                is ReturnBranch -> {
                    this.popScope()
                    return ret
                }

                else -> retainer?.add(ret)
            }
            update()
        }
        popScope()
        return retainer
    }

    open fun visitForOfStatement(node: AST): Any? {

        val forOfNode = node as ForOfStatement
        val collection = visit(forOfNode.collection)
        if (collection !is List<*>) {
            throw RuntimeException("Can iterate non-array object")
        }
        val retainer = if (forOfNode.retain) mutableListOf<Any?>() else null
        pushScope("com.ml.labs.ForOfStatement")
        for (it in collection) {
            _declareForIdentifier(forOfNode.identifier, it)
            when (val ret = visit(forOfNode.body)) {
                is BreakBranch -> break
                is ContinueBranch -> continue
                is ReturnBranch -> {
                    this.popScope()
                    return ret
                }

                else -> retainer?.add(ret)
            }
        }
        popScope()
        return retainer
    }

    open fun visitContinueAST(node: AST): Any {
        return ContinueBranch()
    }

    open fun visitReturnAST(node: AST): Any {
        return ReturnBranch(
            if ((node as ReturnAST).value != null) visit(node.value!!) else null
        )
    }

    private fun resolveArguments(arguments: List<AST>): List<Any?> {
        return arguments.map { visit(it) }
    }

    open fun clone(): MEvento {
        val ret = MEvento()
        ret.copyAttributes(this)
        return ret
    }


    open fun execute(
        source: String,
        cache: Boolean = true,
        input: Map<String, Any?>? = null,
    ): Any? {
        val module = compile(source, cache)
        input?.forEach { (key, value) -> this._changeVariable(key, value) }
        return visit(module)
    }

    companion object {
        val globalFunctionsRegistry: MutableMap<String, (List<Any?>, MEvento?) -> Any?> =
            mutableMapOf()
        val _cache: MutableMap<Int, AST> = mutableMapOf()
        fun compile(source: String, cache: Boolean = false): AST {
            val hash = source.hashCode()
            if (cache && _cache.containsKey(hash)) {
                return _cache[hash]!!
            }
            val lexer = Lexer(source)
            val parser = Parser(lexer)
            val module = parser.parse()
            if (cache) _cache[hash] = module
            return module
        }

        fun register(id: String, fn: (List<Any?>, MEvento?) -> Any?) {
            globalFunctionsRegistry[id] = fn
        }

        fun unregister(id: String) {
            globalFunctionsRegistry.remove(id)
        }

        fun run(source: String, cache: Boolean = false): Any? {
            return MEvento().execute(source, cache)
        }

        fun newInstance(): MEvento {
            return MEvento()
        }
    }

}
