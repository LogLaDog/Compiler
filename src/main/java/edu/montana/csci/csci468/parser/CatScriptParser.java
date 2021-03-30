package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.LinkedList;
import java.util.List;
import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {
//test
    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        if (tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        Statement forStmt = parseForStatement();
        if (forStmt != null) {
            return forStmt;
        }
        Statement ifStmt = parseIfStatement();
        if (ifStmt != null) {
            return ifStmt;
        }
        Statement varStm = parseVariableStatement();
        if(varStm != null){
            return varStm;
        }
        Statement assignStm = parseAssignmentStatement();
        if(assignStm != null){
            return assignStm;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseAssignmentStatement() {
        if (tokens.match(IDENTIFIER)) {
            AssignmentStatement assignmentStatement = new AssignmentStatement();
            Token token = tokens.getCurrentToken();
            assignmentStatement.setStart(tokens.consumeToken());
            assignmentStatement.setVariableName(token.getStringValue());
            require(EQUAL, assignmentStatement);
            assignmentStatement.setExpression(parseExpression());
            assignmentStatement.setEnd(require(RIGHT_PAREN, assignmentStatement));
            return assignmentStatement;
            }
            else {
                return null;
            }
        }

    private Statement parseIfStatement() {
        if (tokens.match(IF)) {
            IfStatement ifStatement = new IfStatement();
            ifStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, ifStatement);
            require(IDENTIFIER, ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStatement);
            require(LEFT_BRACE, ifStatement);
            List<Statement> statements = new LinkedList<>();
            statements.add(parseProgramStatement());
            ifStatement.getTrueStatements();
            Token token = require(RIGHT_BRACE, ifStatement);
            ifStatement.setEnd(token);
            return ifStatement;
        }
        return null;
    }

    private Statement parseVariableStatement() {
        if (tokens.match(VAR)) {
            VariableStatement variableStatement = new VariableStatement();
            variableStatement.setStart(tokens.consumeToken());
            if (tokens.match(IDENTIFIER)) {
                Token token = tokens.getCurrentToken();
                variableStatement.setVariableName(token.getStringValue());
            }

            return variableStatement;
        }
        return null;
    }

    private Statement parseForStatement() {
        if (tokens.match(FOR)) {
            ForStatement forStatement = new ForStatement();
            forStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, forStatement);
            require(IDENTIFIER, forStatement);
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);
            require(LEFT_BRACE, forStatement);
            List<Statement> statements = new LinkedList<>();
            statements.add(parseProgramStatement());
            forStatement.setBody(statements);
            Token token = require(RIGHT_BRACE, forStatement);
            forStatement.setEnd(token);
            return forStatement;
        }
        return null;
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression() {
        Expression lhs = parseComparisonExpression();
        if(tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseEqualityExpression();
            return new EqualityExpression(token, lhs, rhs);
        }
        else {
            return lhs;
        }
    }

    private Expression parseComparisonExpression() {
        Expression lhs = parseAdditiveExpression();
        if(tokens.match(LESS_EQUAL, GREATER_EQUAL, GREATER, LESS)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseComparisonExpression();
            return new ComparisonExpression(token, lhs, rhs);
        }
        else {
            return lhs;
        }
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(STAR, SLASH)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token token = tokens.consumeToken();
            StringLiteralExpression expr = new StringLiteralExpression(token.getStringValue());
            expr.setToken(token);
            return expr;
        } else if (tokens.match(TRUE, FALSE)) {
            Token token = tokens.consumeToken();
            BooleanLiteralExpression expr = new BooleanLiteralExpression(token.getType() == TRUE);
            expr.setToken(token);
            return expr;
        } else if (tokens.match(NULL)) {
            Token token = tokens.consumeToken();
            NullLiteralExpression expr = new NullLiteralExpression();
            expr.setToken(token);
            return expr;
        } else if (tokens.match(IDENTIFIER)) {
            Token start = tokens.consumeToken();
            if (tokens.matchAndConsume(LEFT_PAREN)) {
                return parseFunctionCallExpression(start);
            } else {
                IdentifierExpression expr = new IdentifierExpression(start.getStringValue());
                expr.setToken(start);
                return expr;
            }
        }
            else if (tokens.match(LEFT_BRACKET)) {
            Token start = tokens.consumeToken();
            List<Expression> values = new LinkedList<>();
            if (!tokens.match(RIGHT_BRACKET)) {
                do {
                    values.add(parseExpression());
                } while (tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
            }
            ListLiteralExpression expr = new ListLiteralExpression(values);
            expr.setStart(start);
            expr.setEnd(require(RIGHT_BRACKET, expr, ErrorType.UNTERMINATED_LIST));
            return expr;
        }
            else if (tokens.match(LEFT_PAREN)) {
            Token start = tokens.consumeToken();
            ParenthesizedExpression expr = new ParenthesizedExpression(parseExpression());
            expr.setStart(start);
            if (tokens.match(RIGHT_PAREN)) {
                expr.setEnd(tokens.consumeToken());
            } else {
                expr.setEnd(tokens.consumeToken());
                expr.addError(ErrorType.UNTERMINATED_LIST);
            }
            return expr;
        }
             else {
                 SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
                return syntaxErrorExpression;
            }
    }

    private Expression parseFunctionCallExpression(Token start) {
        List<Expression> values = new LinkedList<>();
        if (!tokens.match(RIGHT_PAREN)) {
            do {
                values.add(parseExpression());
            } while (tokens.matchAndConsume(COMMA) && tokens.hasMoreTokens());
        }
        FunctionCallExpression expr = new FunctionCallExpression("foo", values);
        expr.setStart(start);
        expr.setEnd(require(RIGHT_PAREN, expr, ErrorType.UNTERMINATED_ARG_LIST));
        return expr;
    }


    //============================================================
        //  Parse Helpers
        //============================================================
        private Token require (TokenType type, ParseElement elt){
            return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
        }

        private Token require (TokenType type, ParseElement elt, ErrorType msg){
            if (tokens.match(type)) {
                return tokens.consumeToken();
            } else {
                elt.addError(msg, tokens.getCurrentToken());
                return tokens.getCurrentToken();
            }
        }

    }
