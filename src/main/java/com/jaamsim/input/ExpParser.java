/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.input;

import java.util.ArrayList;

public class ExpParser {

	public interface UnOpFunc {
		public double apply(double val);
	}

	public interface BinOpFunc {
		public double apply(double lval, double rval);
	}

	public interface CallableFunc {
		public double call(double[] args);
	}

	public interface VarTable {
		public double getVariableValue(String[] names);
	}

	////////////////////////////////////////////////////////////////////
	// Expression types

	public interface Expression {
		public double evaluate(VarTable vars);
	}

	public static class Constant implements Expression {
		private double val;
		public Constant(double val) {
			this.val = val;
		}
		@Override
		public double evaluate(VarTable vars) {
			return val;
		}
	}

	public static class Variable implements Expression {
		private String[] vals;
		public Variable(String[] vals) {
			this.vals = vals;
		}
		@Override
		public double evaluate(VarTable vars) {
			return vars.getVariableValue(vals);
		}
	}

	public static class UnaryOp implements Expression {
		private Expression subExp;
		private UnOpFunc func;
		UnaryOp(Expression subExp, UnOpFunc func) {
			this.subExp = subExp;
			this.func = func;
		}

		@Override
		public double evaluate(VarTable vars) {
			return func.apply(subExp.evaluate(vars));
		}
	}

	public static class BinaryOp implements Expression {
		private Expression lSubExp;
		private Expression rSubExp;
		private BinOpFunc func;
		BinaryOp(Expression lSubExp, Expression rSubExp, BinOpFunc func) {
			this.lSubExp = lSubExp;
			this.rSubExp = rSubExp;
			this.func = func;
		}

		@Override
		public double evaluate(VarTable vars) {
			return func.apply(lSubExp.evaluate(vars), rSubExp.evaluate(vars));
		}
	}

	public static class FuncCall implements Expression {
		private ArrayList<Expression> args;
		private CallableFunc function;
		public FuncCall(CallableFunc function, ArrayList<Expression> args) {
			this.function = function;
			this.args = args;
		}

		@Override
		public double evaluate(VarTable vars) {
			double[] argVals = new double[args.size()];
			for (int i = 0; i < args.size(); ++i) {
				argVals[i] = args.get(i).evaluate(vars);
			}
			return function.call(argVals);
		}
	}

	public static class Assignment {
		public String[] destination;
		public Expression value;
	}

	///////////////////////////////////////////////////////////
	// Entries for user definable operators and functions

	private static class UnaryOpEntry {
		public String symbol;
		public UnOpFunc function;
	}

	private static class BinaryOpEntry {
		public String symbol;
		public BinOpFunc function;
		public double bindingPower;
		public boolean rAssoc;
	}

	private static class FunctionEntry {
		public String name;
		public CallableFunc function;
		public int numArgs;
	}

	private static ArrayList<UnaryOpEntry> unaryOps = new ArrayList<UnaryOpEntry>();
	private static ArrayList<BinaryOpEntry> binaryOps = new ArrayList<BinaryOpEntry>();
	private static ArrayList<FunctionEntry> functions = new ArrayList<FunctionEntry>();

	private static void addUnaryOp(String symbol, UnOpFunc func) {
		UnaryOpEntry oe = new UnaryOpEntry();
		oe.symbol = symbol;
		oe.function = func;
		unaryOps.add(oe);
	}

	private static void addBinaryOp(String symbol, double bindPower, boolean rAssoc, BinOpFunc func) {
		BinaryOpEntry oe = new BinaryOpEntry();
		oe.symbol = symbol;
		oe.function = func;
		oe.bindingPower = bindPower;
		oe.rAssoc = rAssoc;
		binaryOps.add(oe);
	}

	private static void addFunction(String name, int numArgs, CallableFunc func) {
		FunctionEntry fe = new FunctionEntry();
		fe.name = name;
		fe.function = func;
		fe.numArgs = numArgs;
		functions.add(fe);
	}

	private static UnaryOpEntry getUnaryOp(String symbol) {
		for (UnaryOpEntry oe: unaryOps) {
			if (oe.symbol.equals(symbol))
				return oe;
		}
		return null;
	}
	private static BinaryOpEntry getBinaryOp(String symbol) {
		for (BinaryOpEntry oe: binaryOps) {
			if (oe.symbol.equals(symbol))
				return oe;
		}
		return null;
	}

	private static FunctionEntry getFunctionEntry(String funcName) {
		for (FunctionEntry fe : functions) {
			if (fe.name.equals(funcName)){
				return fe;
			}
		}
		return null;
	}

	////////////////////////////////////////////////////////
	// Statically initialize the operators and functions

	static {
		addUnaryOp("-", new UnOpFunc() {
			@Override
			public double apply(double val){
				return val * -1;
			}
		});

		addBinaryOp("+", 10, false, new BinOpFunc() {
			@Override
			public double apply(double lval, double rval){
				return lval + rval;
			}
		});

		addBinaryOp("-", 10, false, new BinOpFunc() {
			@Override
			public double apply(double lval, double rval){
				return lval - rval;
			}
		});

		addBinaryOp("*", 20, false, new BinOpFunc() {
			@Override
			public double apply(double lval, double rval){
				return lval * rval;
			}
		});

		addBinaryOp("/", 20, false, new BinOpFunc() {
			@Override
			public double apply(double lval, double rval){
				return lval / rval;
			}
		});

		addBinaryOp("^", 30, true, new BinOpFunc() {
			@Override
			public double apply(double lval, double rval){
				return Math.pow(lval, rval);
			}
		});

		addFunction("max", 2, new CallableFunc() {
			@Override
			public double call(double[] args) {
				return Math.max(args[0], args[1]);
			}
		});

		addFunction("min", 2, new CallableFunc() {
			@Override
			public double call(double[] args) {
				return Math.min(args[0], args[1]);
			}
		});

		addFunction("abs", 1, new CallableFunc() {
			@Override
			public double call(double[] args) {
				return Math.abs(args[0]);
			}
		});

	}

	public static class Error extends Exception {
		Error(String err) {
			super(err);
		}
	}

	/**
	 * A utility class to make dealing with a list of tokens easier
	 *
	 */
	private static class TokenList {
		ArrayList<ExpTokenizer.Token> tokens;
		int pos;

		TokenList(ArrayList<ExpTokenizer.Token> tokens) {
			this.tokens = tokens;
			this.pos = 0;
		}

		public void expect(int type, String val) throws Error {
			ExpTokenizer.Token nextTok = tokens.get(pos);

			if (nextTok.type != type || !nextTok.value.equals(val)) {
				throw new Error(String.format("Expected \"%s\", got \"%s\" at position %d", val, nextTok.value, nextTok.pos));
			}
			pos++;
		}

		public ExpTokenizer.Token next() {
			if (pos >= tokens.size()) {
				return null;
			}
			return tokens.get(pos++);
		}

		public ExpTokenizer.Token peek() {
			if (pos >= tokens.size()) {
				return null;
			}
			return tokens.get(pos);
		}
	}

	/**
	 * The main entry point to the expression parsing system, will either return a valid
	 * expression that can be evaluated, or throw an error.
	 */
	public static Expression parseExpression(String input) throws Error {
		ArrayList<ExpTokenizer.Token> ts;
		try {
			ts = ExpTokenizer.tokenize(input);
		} catch (ExpTokenizer.Error ex){
			throw new Error(ex.getMessage());
		}

		TokenList tokens = new TokenList(ts);

		Expression exp = parseExp(tokens, 0);

		// Make sure we've parsed all the tokens
		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked != null) {
			throw new Error(String.format("Unexpected additional values at position: %d", peeked.pos));
		}

		return exp;
	}

	private static Expression parseExp(TokenList tokens, double bindPower) throws Error {
		Expression lhs = parseOpeningExp(tokens, bindPower);
		// Now peek for a binary op to modify this expression

		while (true) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE) {
				break;
			}
			BinaryOpEntry oe = getBinaryOp(peeked.value);
			if (oe == null || oe.bindingPower <= bindPower) {
				break;
			}
			// The next token is a binary op and powerful enough to bind us
			tokens.next(); // Consume the operator

			// For right associative operators, we weaken the binding power a bit at application time (but not testing time)
			double assocMod = oe.rAssoc ? -0.5 : 0;
			Expression rhs = parseExp(tokens, oe.bindingPower + assocMod);
			//currentPower = oe.bindingPower;

			lhs = new BinaryOp(lhs, rhs, oe.function);
		}

		// We have bound as many operators as we can, return it
		return lhs;
	}

	public static Assignment parseAssignment(String input) throws Error {
		ArrayList<ExpTokenizer.Token> ts;
		try {
			ts = ExpTokenizer.tokenize(input);
		} catch (ExpTokenizer.Error ex){
			throw new Error(ex.getMessage());
		}

		TokenList tokens = new TokenList(ts);

		ExpTokenizer.Token nextTok = tokens.next();
		if (nextTok == null || nextTok.type != ExpTokenizer.VAR_TYPE) {
			throw new Error("Assignments must start with an identifier");
		}
		ArrayList<String> destination = parseIdentifier(nextTok, tokens);

		nextTok = tokens.next();
		if (nextTok == null || nextTok.type != ExpTokenizer.SYM_TYPE || !nextTok.value.equals("=")) {
			throw new Error("Expected '=' in assignment");
		}

		Expression exp = parseExp(tokens, 0);

		Assignment ret = new Assignment();
		ret.destination = destination.toArray(STRING_ARRAY_TYPE);
		ret.value = exp;
		return ret;
	}

	// Static array to make ArrayList.toArray work
	private static final String[] STRING_ARRAY_TYPE = new String[0];

	// The first half of expression parsing, parse a simple expression based on the next token
	private static Expression parseOpeningExp(TokenList tokens, double bindPower) throws Error{
		ExpTokenizer.Token nextTok = tokens.next(); // consume the first token

		if (nextTok == null) {
			throw new Error("Unexpected end of string");
		}

		if (nextTok.type == ExpTokenizer.NUM_TYPE) {
			return new Constant(Double.parseDouble(nextTok.value));
		}
		if (nextTok.type == ExpTokenizer.VAR_TYPE) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked != null && peeked.type == ExpTokenizer.SYM_TYPE && peeked.value.equals("(")) {
				return parseFuncCall(nextTok.value, tokens);
			}
			// If not a function call, must be variable evaluation
			ArrayList<String> vals = parseIdentifier(nextTok, tokens);
			return new Variable(vals.toArray(STRING_ARRAY_TYPE));
		}

		// The next token must be a symbol

		// handle parenthesis
		if (nextTok.value.equals("(")) {
			Expression exp = parseExp(tokens, 0);
			tokens.expect(ExpTokenizer.SYM_TYPE, ")"); // Expect the closing paren
			return exp;
		}

		UnaryOpEntry oe = getUnaryOp(nextTok.value);
		if (oe != null) {
			Expression exp = parseExp(tokens, bindPower);
			return new UnaryOp(exp, oe.function);
		}

		// We're all out of tricks here, this is an unknown expression
		throw new Error(String.format("Can not parse expression at %d", nextTok.pos));
	}

	private static Expression parseFuncCall(String funcName, TokenList tokens) throws Error {

		tokens.expect(ExpTokenizer.SYM_TYPE, "(");
		ArrayList<Expression> arguments = new ArrayList<Expression>();

		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked == null) {
			throw new Error("Unexpected end of input in argument list");
		}
		boolean isEmpty = false;
		if (peeked.value.equals(")")) {
			// Special case with empty argument list
			isEmpty = true;
			tokens.next(); // Consume closing parens
		}

		while (!isEmpty) {
			Expression nextArg = parseExp(tokens, 0);
			arguments.add(nextArg);

			ExpTokenizer.Token nextTok = tokens.next();
			if (nextTok == null) {
				throw new Error("Unexpected end of input in argument list.");
			}
			if (nextTok.value.equals(")")) {
				break;
			}

			if (nextTok.value.equals(",")) {
				continue;
			}

			// Unexpected token
			throw new Error(String.format("Unexpected token in arguement list at: %d", nextTok.pos));
		}

		FunctionEntry fe = getFunctionEntry(funcName);
		if (fe == null) {
			throw new Error(String.format("Uknown function: \"%s\"", funcName));
		}

		if (fe.numArgs != arguments.size()){
			throw new Error(String.format("Function \"%s\" expects %d arguments. %d provided.",
					funcName, fe.numArgs, arguments.size()));
		}
		return new FuncCall(fe.function, arguments);
	}

	private static ArrayList<String> parseIdentifier(ExpTokenizer.Token firstName, TokenList tokens) throws Error {
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(firstName.value);
		while (true) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE || !peeked.value.equals(".")) {
				break;
			}
			// Next token is a '.' so parse another name

			tokens.next(); // consume
			ExpTokenizer.Token nextName = tokens.next();
			if (nextName == null || nextName.type != ExpTokenizer.VAR_TYPE) {
				throw new Error(String.format("Expected Identifier after '.' at pos: %d", peeked.pos));
			}

			vals.add(nextName.value);
		}

		return vals;
	}
}
