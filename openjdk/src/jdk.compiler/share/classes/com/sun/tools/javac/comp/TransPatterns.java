/*
 * Copyright (c) 2017, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.tools.javac.comp;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CaseTree.CaseKind;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Kinds.Kind;
import com.sun.tools.javac.code.Preview;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.BindingSymbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.DynamicMethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.Type.WildcardType;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCBindingPattern;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Options;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.LinkedHashMap;

import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.RecordComponent;
import com.sun.tools.javac.code.Type;
import static com.sun.tools.javac.code.TypeTag.BOT;
import com.sun.tools.javac.jvm.PoolConstant.LoadableConstant;
import com.sun.tools.javac.jvm.Target;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCaseLabel;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCConstantCaseLabel;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCParenthesizedPattern;
import com.sun.tools.javac.tree.JCTree.JCPattern;
import com.sun.tools.javac.tree.JCTree.JCPatternCaseLabel;
import com.sun.tools.javac.tree.JCTree.JCRecordPattern;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCSwitchExpression;
import com.sun.tools.javac.tree.JCTree.JCYield;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;

/**
 * This pass translates pattern-matching constructs, such as instanceof <pattern>.
 */
public class TransPatterns extends TreeTranslator {

    protected static final Context.Key<TransPatterns> transPatternsKey = new Context.Key<>();

    public static TransPatterns instance(Context context) {
        TransPatterns instance = context.get(transPatternsKey);
        if (instance == null)
            instance = new TransPatterns(context);
        return instance;
    }

    private final Symtab syms;
    private final Attr attr;
    private final Resolve rs;
    private final Types types;
    private final Operators operators;
    private final Names names;
    private final Target target;
    private final Preview preview;
    private TreeMaker make;
    private Env<AttrContext> env;

    BindingContext bindingContext = new BindingContext() {
        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            return null;
        }

        @Override
        VarSymbol getBindingFor(BindingSymbol varSymbol) {
            return null;
        }

        @Override
        List<JCStatement> bindingVars(int diagPos) {
            return List.nil();
        }

        @Override
        JCStatement decorateStatement(JCStatement stat) {
            return stat;
        }

        @Override
        JCExpression decorateExpression(JCExpression expr) {
            return expr;
        }

        @Override
        BindingContext pop() {
            //do nothing
            return this;
        }

        @Override
        boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
            return false;
        }
    };

    JCLabeledStatement pendingMatchLabel = null;

    boolean debugTransPatterns;

    private ClassSymbol currentClass = null;
    private JCClassDecl currentClassTree = null;
    private ListBuffer<JCTree> pendingMethods = null;
    private MethodSymbol currentMethodSym = null;
    private VarSymbol currentValue = null;
    private Map<RecordComponent, MethodSymbol> component2Proxy = null;

    protected TransPatterns(Context context) {
        context.put(transPatternsKey, this);
        syms = Symtab.instance(context);
        attr = Attr.instance(context);
        rs = Resolve.instance(context);
        make = TreeMaker.instance(context);
        types = Types.instance(context);
        operators = Operators.instance(context);
        names = Names.instance(context);
        target = Target.instance(context);
        preview = Preview.instance(context);
        debugTransPatterns = Options.instance(context).isSet("debug.patterns");
    }

    @Override
    public void visitTypeTest(JCInstanceOf tree) {
        if (tree.pattern instanceof JCPattern) {
            //E instanceof $pattern
            //=>
            //(let T' N$temp = E; N$temp instanceof typeof($pattern) && <desugared $pattern>)
            //note the pattern desugaring performs binding variable assignments
            Type tempType = tree.expr.type.hasTag(BOT) ?
                    syms.objectType
                    : tree.expr.type;
            VarSymbol prevCurrentValue = currentValue;
            bindingContext = new BasicBindingContext();
            try {
                JCExpression translatedExpr = translate(tree.expr);
                Symbol exprSym = TreeInfo.symbol(translatedExpr);

                if (exprSym != null &&
                    exprSym.kind == Kind.VAR &&
                    exprSym.owner.kind.matches(Kinds.KindSelector.VAL_MTH)) {
                    currentValue = (VarSymbol) exprSym;
                } else {
                    currentValue = new VarSymbol(Flags.FINAL | Flags.SYNTHETIC,
                            names.fromString("patt" + tree.pos + target.syntheticNameChar() + "temp"),
                            tempType,
                            currentMethodSym);
                }

                Type principalType = principalType((JCPattern) tree.pattern);
                JCExpression resultExpression=
                        makeBinary(Tag.AND,
                                   makeTypeTest(make.Ident(currentValue), make.Type(principalType)),
                                   (JCExpression) this.<JCTree>translate(tree.pattern));
                if (currentValue != exprSym) {
                    resultExpression =
                            make.at(tree.pos).LetExpr(make.VarDef(currentValue, translatedExpr),
                                                      resultExpression).setType(syms.booleanType);
                    ((LetExpr) resultExpression).needsCond = true;
                }
                result = bindingContext.decorateExpression(resultExpression);
            } finally {
                currentValue = prevCurrentValue;
                bindingContext.pop();
            }
        } else {
            super.visitTypeTest(tree);
        }
    }

    @Override
    public void visitBindingPattern(JCBindingPattern tree) {
        //it is assumed the primary type has already been checked:
        BindingSymbol binding = (BindingSymbol) tree.var.sym;
        Type castTargetType = principalType(tree);
        VarSymbol bindingVar = bindingContext.bindingDeclared(binding);

        if (bindingVar != null) {
            JCAssign fakeInit = (JCAssign)make.at(TreeInfo.getStartPos(tree)).Assign(
                    make.Ident(bindingVar), convert(make.Ident(currentValue), castTargetType)).setType(bindingVar.erasure(types));
            LetExpr nestedLE = make.LetExpr(List.of(make.Exec(fakeInit)),
                                            make.Literal(true));
            nestedLE.needsCond = true;
            nestedLE.setType(syms.booleanType);
            result = nestedLE;
        } else {
            result = make.Literal(true);
        }
    }

    @Override
    public void visitParenthesizedPattern(JCParenthesizedPattern tree) {
        result = translate(tree.pattern);
    }

    @Override
    public void visitRecordPattern(JCRecordPattern tree) {
        //type test already done, finish handling of deconstruction patterns ("T(PATT1, PATT2, ...)")
        //=>
        //<PATT1-handling> && <PATT2-handling> && ...
        List<? extends RecordComponent> components = tree.record.getRecordComponents();
        List<? extends Type> nestedFullComponentTypes = tree.fullComponentTypes;
        List<? extends JCPattern> nestedPatterns = tree.nested;
        JCExpression test = null;
        while (components.nonEmpty() && nestedFullComponentTypes.nonEmpty() && nestedPatterns.nonEmpty()) {
            //PATTn for record component COMPn of type Tn;
            //PATTn is a type test pattern or a deconstruction pattern:
            //=>
            //(let Tn $c$COMPn = ((T) N$temp).COMPn(); <PATTn extractor>)
            //or
            //(let Tn $c$COMPn = ((T) N$temp).COMPn(); $c$COMPn != null && <PATTn extractor>)
            //or
            //(let Tn $c$COMPn = ((T) N$temp).COMPn(); $c$COMPn instanceof T' && <PATTn extractor>)
            RecordComponent component = components.head;
            JCPattern nested = nestedPatterns.head;
            VarSymbol nestedTemp = new VarSymbol(Flags.SYNTHETIC,
                names.fromString(target.syntheticNameChar() + "c" + target.syntheticNameChar() + component.name),
                                 component.erasure(types),
                                 currentMethodSym);
            Symbol accessor = getAccessor(tree.pos(), component);
            JCVariableDecl nestedTempVar =
                    make.VarDef(nestedTemp,
                                make.App(make.QualIdent(accessor),
                                         List.of(convert(make.Ident(currentValue), tree.type))));
            JCExpression extracted;
            VarSymbol prevCurrentValue = currentValue;
            try {
                currentValue = nestedTemp;
                extracted = (JCExpression) this.<JCTree>translate(nested);
            } finally {
                currentValue = prevCurrentValue;
            }
            JCExpression extraTest = null;
            if (!types.isAssignable(nestedTemp.type, nested.type)) {
                if (!types.isAssignable(nestedFullComponentTypes.head, nested.type)) {
                    extraTest = makeTypeTest(make.Ident(nestedTemp),
                                             make.Type(nested.type));
                }
            } else if (nested.type.isReference() && nested.hasTag(Tag.RECORDPATTERN)) {
                extraTest = makeBinary(Tag.NE, make.Ident(nestedTemp), makeNull());
            }
            if (extraTest != null) {
                extracted = makeBinary(Tag.AND, extraTest, extracted);
            }
            LetExpr getAndRun = make.LetExpr(nestedTempVar, extracted);
            getAndRun.needsCond = true;
            getAndRun.setType(syms.booleanType);
            if (test == null) {
                test = getAndRun;
            } else {
                test = makeBinary(Tag.AND, test, getAndRun);
            }
            components = components.tail;
            nestedFullComponentTypes = nestedFullComponentTypes.tail;
            nestedPatterns = nestedPatterns.tail;
        }

        if (tree.var != null) {
            BindingSymbol binding = (BindingSymbol) tree.var.sym;
            Type castTargetType = principalType(tree);
            VarSymbol bindingVar = bindingContext.bindingDeclared(binding);

            JCAssign fakeInit =
                    (JCAssign) make.at(TreeInfo.getStartPos(tree))
                                   .Assign(make.Ident(bindingVar),
                                           convert(make.Ident(currentValue), castTargetType))
                                   .setType(bindingVar.erasure(types));
            LetExpr nestedLE = make.LetExpr(List.of(make.Exec(fakeInit)),
                                            make.Literal(true));
            nestedLE.needsCond = true;
            nestedLE.setType(syms.booleanType);
            test = test != null ? makeBinary(Tag.AND, test, nestedLE) : nestedLE;
        }

        Assert.check(components.isEmpty() == nestedPatterns.isEmpty());
        Assert.check(components.isEmpty() == nestedFullComponentTypes.isEmpty());
        result = test != null ? test : makeLit(syms.booleanType, 1);
    }

    private MethodSymbol getAccessor(DiagnosticPosition pos, RecordComponent component) {
        return component2Proxy.computeIfAbsent(component, c -> {
            MethodType type = new MethodType(List.of(component.owner.erasure(types)),
                                             types.erasure(component.type),
                                             List.nil(),
                                             syms.methodClass);
            MethodSymbol proxy = new MethodSymbol(Flags.PRIVATE | Flags.STATIC | Flags.SYNTHETIC,
                                                  names.fromString("$proxy$" + component.name),
                                                  type,
                                                  currentClass);
            JCStatement accessorStatement =
                    make.Return(make.App(make.Select(make.Ident(proxy.params().head), c.accessor)));
            VarSymbol ctch = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString("catch" + currentClassTree.pos + target.syntheticNameChar()),
                    syms.throwableType,
                    currentMethodSym);
            JCNewClass newException = makeNewClass(inferUsableMatchExceptionType(),
                                                   List.of(makeApply(make.Ident(ctch),
                                                                     names.toString,
                                                                     List.nil()),
                                                           make.Ident(ctch)));
            JCTree.JCCatch catchClause = make.Catch(make.VarDef(ctch, null),
                                                    make.Block(0, List.of(make.Throw(newException))));
            JCStatement tryCatchAll = make.Try(make.Block(0, List.of(accessorStatement)),
                                               List.of(catchClause),
                                               null);
            JCMethodDecl md = make.MethodDef(proxy,
                                             proxy.externalType(types),
                                             make.Block(0, List.of(tryCatchAll)));

            pendingMethods.append(md);
            currentClass.members().enter(proxy);

            return proxy;
        });
    }

    private Type inferUsableMatchExceptionType() {
        //when MatchException is final/non-preview, we should opt to use it:
        return syms.illegalStateExceptionType;
    }

    @Override
    public void visitSwitch(JCSwitch tree) {
        handleSwitch(tree, tree.selector, tree.cases,
                     tree.hasUnconditionalPattern, tree.patternSwitch);
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression tree) {
        handleSwitch(tree, tree.selector, tree.cases,
                     tree.hasUnconditionalPattern, tree.patternSwitch);
    }

    private void handleSwitch(JCTree tree,
                              JCExpression selector,
                              List<JCCase> cases,
                              boolean hasUnconditionalPattern,
                              boolean patternSwitch) {
        if (patternSwitch) {
            Type seltype = selector.type.hasTag(BOT)
                    ? syms.objectType
                    : selector.type;
            Assert.check(preview.isEnabled());
            Assert.check(preview.usesPreview(env.toplevel.sourcefile));

            //rewrite pattern matching switches:
            //switch ($obj) {
            //     case $constant: $stats$
            //     case $pattern1: $stats$
            //     case $pattern2, null: $stats$
            //     case $pattern3: $stats$
            //}
            //=>
            //int $idx = 0;
            //$RESTART: switch (invokeDynamic typeSwitch($constant, typeof($pattern1), typeof($pattern2), typeof($pattern3))($obj, $idx)) {
            //     case 0:
            //         if (!(<desugared $pattern1>)) { $idx = 1; continue $RESTART; }
            //         $stats$
            //     case 1:
            //         if (!(<desugared $pattern1>)) { $idx = 2; continue $RESTART; }
            //         $stats$
            //     case 2, -1:
            //         if (!(<desugared $pattern1>)) { $idx = 3; continue $RESTART; }
            //         $stats$
            //     case 3:
            //         if (!(<desugared $pattern1>)) { $idx = 4; continue $RESTART; }
            //         $stats$
            //}
            //notes:
            //-pattern desugaring performs assignment to the binding variables
            //-the selector is evaluated only once and stored in a temporary variable
            //-typeSwitch bootstrap method can restart matching at specified index. The bootstrap will
            // categorize the input, and return the case index whose type or constant matches the input.
            // The bootstrap does not evaluate guards, which are injected at the beginning of the case's
            // statement list, and if the guard fails, the switch is "continued" and matching is
            // restarted from the next index.
            //-case null is always desugared to case -1, as the typeSwitch bootstrap method will
            // return -1 when the input is null
            //
            //note the selector is evaluated only once and stored in a temporary variable
            ListBuffer<JCCase> newCases = new ListBuffer<>();
            for (List<JCCase> c = cases; c.nonEmpty(); c = c.tail) {
                if (c.head.stats.isEmpty() && c.tail.nonEmpty()) {
                    c.tail.head.labels = c.tail.head.labels.prependList(c.head.labels);
                } else {
                    newCases.add(c.head);
                }
            }
            cases = newCases.toList();
            ListBuffer<JCStatement> statements = new ListBuffer<>();
            VarSymbol temp = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString("selector" + tree.pos + target.syntheticNameChar() + "temp"),
                    seltype,
                    currentMethodSym);
            boolean hasNullCase = cases.stream()
                                       .flatMap(c -> c.labels.stream())
                                       .anyMatch(p -> TreeInfo.isNullCaseLabel(p));

            JCCase lastCase = cases.last();

            selector = translate(selector);
            boolean needsNullCheck = !hasNullCase && !seltype.isPrimitive();
            statements.append(make.at(tree.pos).VarDef(temp, needsNullCheck ? attr.makeNullCheck(selector)
                                                                            : selector));
            VarSymbol index = new VarSymbol(Flags.SYNTHETIC,
                    names.fromString(tree.pos + target.syntheticNameChar() + "index"),
                    syms.intType,
                    currentMethodSym);
            statements.append(make.at(tree.pos).VarDef(index, makeLit(syms.intType, 0)));

            boolean enumSelector = seltype.tsym.isEnum();

            if (false) {
            List<Type> staticArgTypes = List.of(syms.methodHandleLookupType,
                                                syms.stringType,
                                                syms.methodTypeType,
                                                types.makeArrayType(new ClassType(syms.classType.getEnclosingType(),
                                                                    List.of(new WildcardType(syms.objectType, BoundKind.UNBOUND,
                                                                                             syms.boundClass)),
                                                                    syms.classType.tsym)));
            LoadableConstant[] staticArgValues =
                    cases.stream()
                         .flatMap(c -> c.labels.stream())
                         .map(l -> toLoadableConstant(l, seltype))
                         .filter(c -> c != null)
                         .toArray(s -> new LoadableConstant[s]);

            Name bootstrapName = enumSelector ? names.enumSwitch : names.typeSwitch;
            MethodSymbol bsm = rs.resolveInternalMethod(tree.pos(), env, syms.switchBootstrapsType,
                    bootstrapName, staticArgTypes, List.nil());

            MethodType indyType = new MethodType(
                    List.of(enumSelector ? seltype : syms.objectType, syms.intType),
                    syms.intType,
                    List.nil(),
                    syms.methodClass
            );
            DynamicMethodSymbol dynSym = new DynamicMethodSymbol(bootstrapName,
                    syms.noSymbol,
                    bsm.asHandle(),
                    indyType,
                    staticArgValues);

            JCFieldAccess qualifier = make.Select(make.QualIdent(bsm.owner), dynSym.name);
            qualifier.sym = dynSym;
            qualifier.type = syms.intType;
            selector = make.Apply(List.nil(),
                                  qualifier,
                                  List.of(make.Ident(temp), make.Ident(index)))
                           .setType(syms.intType);
            } else {
                int i = 0;
                boolean hasDefault = false;
                ListBuffer<JCCase> filterCases = new ListBuffer<>();
                JCSwitchExpression newSelector = (JCSwitchExpression) make.SwitchExpression(make.Ident(index), List.nil()).setType(syms.intType);
                for (JCCase c : cases) {
                    for (JCCaseLabel l : c.labels) {
                        if (l.hasTag(Tag.DEFAULTCASELABEL)) {
                            hasDefault = true;
                        } else if (hasUnconditionalPattern && !hasDefault &&
                                   c == lastCase && l instanceof JCPatternCaseLabel) {
                            //If the switch has total pattern, the last case will contain it.
                            //Convert the total pattern to default:
                        } else {
                            int value;
                            if (TreeInfo.isNullCaseLabel(l)) {
                                value = -1;
                            } else {
                                JCExpression test;
                                if (l instanceof JCPatternCaseLabel pcl) {
                                    Type principalType = principalType(pcl.pat);
                                    if (types.isSubtype(seltype, principalType)) {
                                        principalType = seltype;
                                    }
                                    test = make.TypeTest(make.Ident(temp), make.QualIdent(principalType.tsym)).setType(syms.booleanType);
                                } else if (l instanceof JCConstantCaseLabel ccl) {
                                    JCExpression constantLabel = ccl.expr;
                                    JCExpression constant;

                                    if ((constantLabel.type.tsym.flags_field & Flags.ENUM) != 0) {
                                        constant = make.QualIdent(TreeInfo.symbol(constantLabel));
                                    } else {
                                        Assert.checkNonNull(constantLabel.type.constValue());

                                        constant = makeLit(constantLabel.type, constantLabel.type.constValue());
                                    }

                                    if (constantLabel.type.tsym == syms.stringType.tsym) {
                                        Symbol equals = syms.objectsType.tsym.members().getSymbolsByName(names.equals, s -> s.kind == Kind.MTH && ((MethodSymbol) s).params.size() == 2).iterator().next(); //XXX: more careful checks!
                                        test = make.Apply(List.nil(), make.Select(make.QualIdent(syms.objectsType.tsym), equals), List.of(make.Ident(temp), constant)).setType(syms.booleanType);
                                    } else {
                                        test = makeBinary(Tag.EQ, make.Ident(temp), constant);
                                    }
                                } else {
                                    Assert.error();
                                    throw new Error();
                                }

                                JCYield yield = make.Yield(makeLit(syms.intType, i));

                                yield.target = newSelector;
                                filterCases.add(make.Case(CaseKind.STATEMENT, List.of(make.ConstantCaseLabel(makeLit(syms.intType, i))), List.of(make.If(test, yield, null)), null));

                                i++;
                            }
                        }
                    }
                }
                JCYield yield = make.Yield(makeLit(syms.intType, i));
                yield.target = newSelector;
                filterCases.add(make.Case(CaseKind.STATEMENT, List.of(make.DefaultCaseLabel()), List.of(yield), null));
                newSelector.cases = filterCases.toList();
                selector = make.Conditional(makeBinary(Tag.EQ, make.Ident(temp), makeLit(syms.botType, null)), makeLit(syms.intType, -1), newSelector).setType(syms.intType);
            }

            int i = 0;
            boolean previousCompletesNormally = false;
            boolean hasDefault = false;

            for (var c : cases) {
                List<JCCaseLabel> clearedPatterns = c.labels;
                boolean hasJoinedNull =
                        c.labels.size() > 1 && c.labels.stream().anyMatch(l -> TreeInfo.isNullCaseLabel(l));
                if (hasJoinedNull) {
                    clearedPatterns = c.labels.stream()
                                              .filter(l -> !TreeInfo.isNullCaseLabel(l))
                                              .collect(List.collector());
                }
                if (clearedPatterns.size() == 1 && clearedPatterns.head.hasTag(Tag.PATTERNCASELABEL) && !previousCompletesNormally) {
                    JCPatternCaseLabel label = (JCPatternCaseLabel) clearedPatterns.head;
                    bindingContext = new BasicBindingContext();
                    VarSymbol prevCurrentValue = currentValue;
                    try {
                        currentValue = temp;
                        JCExpression test = (JCExpression) this.<JCTree>translate(label.pat);
                        if (label.guard != null) {
                            JCExpression guard = translate(label.guard);
                            if (hasJoinedNull) {
                                JCPattern pattern = label.pat;
                                while (pattern instanceof JCParenthesizedPattern parenthesized) {
                                    pattern = parenthesized.pattern;
                                }
                                Assert.check(pattern.hasTag(Tag.BINDINGPATTERN));
                                VarSymbol binding = ((JCBindingPattern) pattern).var.sym;
                                guard = makeBinary(Tag.OR,
                                                   makeBinary(Tag.EQ,
                                                              make.Ident(binding),
                                                              makeNull()),
                                                   guard);
                            }
                            test = makeBinary(Tag.AND, test, guard);
                        }
                        c.stats = translate(c.stats);
                        JCContinue continueSwitch = make.at(clearedPatterns.head.pos()).Continue(null);
                        continueSwitch.target = tree;
                        c.stats = c.stats.prepend(make.If(makeUnary(Tag.NOT, test).setType(syms.booleanType),
                                                           make.Block(0, List.of(make.Exec(make.Assign(make.Ident(index),
                                                                                                       makeLit(syms.intType, i + 1))
                                                                                     .setType(syms.intType)),
                                                                                 continueSwitch)),
                                                           null));
                        c.stats = c.stats.prependList(bindingContext.bindingVars(c.pos));
                    } finally {
                        currentValue = prevCurrentValue;
                        bindingContext.pop();
                    }
                } else {
                    c.stats = translate(c.stats);
                }
                ListBuffer<JCCaseLabel> translatedLabels = new ListBuffer<>();
                for (var p : c.labels) {
                    if (p.hasTag(Tag.DEFAULTCASELABEL)) {
                        translatedLabels.add(p);
                        hasDefault = true;
                    } else if (hasUnconditionalPattern && !hasDefault &&
                               c == lastCase && p.hasTag(Tag.PATTERNCASELABEL)) {
                        //If the switch has unconditional pattern,
                        //the last case will contain it.
                        //Convert the unconditional pattern to default:
                        translatedLabels.add(make.DefaultCaseLabel());
                    } else {
                        int value;
                        if (TreeInfo.isNullCaseLabel(p)) {
                            value = -1;
                        } else {
                            value = i++;
                        }
                        translatedLabels.add(make.ConstantCaseLabel(make.Literal(value)));
                    }
                }
                c.labels = translatedLabels.toList();
                if (c.caseKind == CaseTree.CaseKind.STATEMENT) {
                    previousCompletesNormally = c.completesNormally;
                } else {
                    previousCompletesNormally = false;
                    JCBreak brk = make.at(TreeInfo.endPos(c.stats.last())).Break(null);
                    brk.target = tree;
                    c.stats = c.stats.append(brk);
                }
            }

            if (tree.hasTag(Tag.SWITCH)) {
                ((JCSwitch) tree).selector = selector;
                ((JCSwitch) tree).cases = cases;
                ((JCSwitch) tree).wasEnumSelector = enumSelector;
                statements.append((JCSwitch) tree);
                result = make.Block(0, statements.toList());
            } else {
                ((JCSwitchExpression) tree).selector = selector;
                ((JCSwitchExpression) tree).cases = cases;
                ((JCSwitchExpression) tree).wasEnumSelector = enumSelector;
                LetExpr r = (LetExpr) make.LetExpr(statements.toList(), (JCSwitchExpression) tree)
                                          .setType(tree.type);

                r.needsCond = true;
                result = r;
            }
            return ;
        }
        if (tree.hasTag(Tag.SWITCH)) {
            super.visitSwitch((JCSwitch) tree);
        } else {
            super.visitSwitchExpression((JCSwitchExpression) tree);
        }
    }

    JCMethodInvocation makeApply(JCExpression selector, Name name, List<JCExpression> args) {
        MethodSymbol method = rs.resolveInternalMethod(
                currentClassTree.pos(), env,
                selector.type, name,
                TreeInfo.types(args), List.nil());
        JCMethodInvocation tree = make.App( make.Select(selector, method), args)
                                      .setType(types.erasure(method.getReturnType()));
        return tree;
    }

    JCNewClass makeNewClass(Type ctype, List<JCExpression> args) {
        JCNewClass tree = make.NewClass(null,
            null, make.QualIdent(ctype.tsym), args, null);
        tree.constructor = rs.resolveConstructor(
            currentClassTree.pos(), this.env, ctype, TreeInfo.types(args), List.nil());
        tree.type = ctype;
        return tree;
    }

    private Type principalType(JCTree p) {
        return types.boxedTypeOrType(types.erasure(TreeInfo.primaryPatternType(p)));
    }

    private LoadableConstant toLoadableConstant(JCCaseLabel l, Type selector) {
        if (l.hasTag(Tag.PATTERNCASELABEL)) {
            Type principalType = principalType(((JCPatternCaseLabel) l).pat);
            if (types.isSubtype(selector, principalType)) {
                return (LoadableConstant) selector;
            } else {
                return (LoadableConstant) principalType;
            }
        } else if (l.hasTag(Tag.CONSTANTCASELABEL)&& !TreeInfo.isNullCaseLabel(l)) {
            JCExpression expr = ((JCConstantCaseLabel) l).expr;
            if ((expr.type.tsym.flags_field & Flags.ENUM) != 0) {
                return LoadableConstant.String(((JCIdent) expr).name.toString());
            } else {
                Assert.checkNonNull(expr.type.constValue());

                return switch (expr.type.getTag()) {
                    case BYTE, CHAR,
                         SHORT, INT -> LoadableConstant.Int((Integer) expr.type.constValue());
                    case CLASS -> LoadableConstant.String((String) expr.type.constValue());
                    default -> throw new AssertionError();
                };
            }
        } else {
            return null;
        }
    }

    @Override
    public void visitBinary(JCBinary tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitBinary(tree);
            result = bindingContext.decorateExpression(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitConditional(JCConditional tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitConditional(tree);
            result = bindingContext.decorateExpression(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitIf(JCIf tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitIf(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitForLoop(JCForLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitForLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitWhileLoop(JCWhileLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitWhileLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop tree) {
        bindingContext = new BasicBindingContext();
        try {
            super.visitDoLoop(tree);
            result = bindingContext.decorateStatement(tree);
        } finally {
            bindingContext.pop();
        }
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            currentMethodSym = tree.sym;
            super.visitMethodDef(tree);
        } finally {
            currentMethodSym = prevMethodSym;
        }
    }

    @Override
    public void visitIdent(JCIdent tree) {
        VarSymbol bindingVar = null;
        if ((tree.sym.flags() & Flags.MATCH_BINDING) != 0) {
            bindingVar = bindingContext.getBindingFor((BindingSymbol)tree.sym);
        }
        if (bindingVar == null) {
            super.visitIdent(tree);
        } else {
            result = make.at(tree.pos).Ident(bindingVar);
        }
    }

    @Override
    public void visitBlock(JCBlock tree) {
        ListBuffer<JCStatement> statements = new ListBuffer<>();
        bindingContext = new BindingDeclarationFenceBindingContext() {
            boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
                //{
                //    if (E instanceof T N) {
                //        return ;
                //    }
                //    //use of N:
                //}
                //=>
                //{
                //    T N;
                //    if ((let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp))) {
                //        return ;
                //    }
                //    //use of N:
                //}
                hoistedVarMap.put(binding, var.sym);
                statements.append(var);
                return true;
            }
        };
        MethodSymbol oldMethodSym = currentMethodSym;
        try {
            if (currentMethodSym == null) {
                // Block is a static or instance initializer.
                currentMethodSym =
                    new MethodSymbol(tree.flags | Flags.BLOCK,
                                     names.empty, null,
                                     currentClass);
            }
            for (List<JCStatement> l = tree.stats; l.nonEmpty(); l = l.tail) {
                statements.append(translate(l.head));
            }

            tree.stats = statements.toList();
            result = tree;
        } finally {
            currentMethodSym = oldMethodSym;
            bindingContext.pop();
        }
    }

    @Override
    public void visitLambda(JCLambda tree) {
        BindingContext prevContent = bindingContext;
        try {
            bindingContext = new BindingDeclarationFenceBindingContext();
            super.visitLambda(tree);
        } finally {
            bindingContext = prevContent;
        }
    }

    @Override
    public void visitClassDef(JCClassDecl tree) {
        ClassSymbol prevCurrentClass = currentClass;
        JCClassDecl prevCurrentClassTree = currentClassTree;
        ListBuffer<JCTree> prevPendingMethods = pendingMethods;
        MethodSymbol prevMethodSym = currentMethodSym;
        Map<RecordComponent, MethodSymbol> prevAccessor2Proxy = component2Proxy;
        try {
            currentClass = tree.sym;
            currentClassTree = tree;
            pendingMethods = new ListBuffer<>();
            currentMethodSym = null;
            component2Proxy = new HashMap<>();
            super.visitClassDef(tree);
            tree.defs = tree.defs.prependList(pendingMethods.toList());
        } finally {
            currentClass = prevCurrentClass;
            currentClassTree = prevCurrentClassTree;
            pendingMethods = prevPendingMethods;
            currentMethodSym = prevMethodSym;
            component2Proxy = prevAccessor2Proxy;
        }
    }

    public void visitVarDef(JCVariableDecl tree) {
        MethodSymbol prevMethodSym = currentMethodSym;
        try {
            tree.mods = translate(tree.mods);
            tree.vartype = translate(tree.vartype);
            if (currentMethodSym == null) {
                // A class or instance field initializer.
                currentMethodSym =
                    new MethodSymbol((tree.mods.flags&Flags.STATIC) | Flags.BLOCK,
                                     names.empty, null,
                                     currentClass);
            }
            if (tree.init != null) tree.init = translate(tree.init);
            result = tree;
        } finally {
            currentMethodSym = prevMethodSym;
        }
    }

    public JCTree translateTopLevelClass(Env<AttrContext> env, JCTree cdef, TreeMaker make) {
        try {
            this.make = make;
            this.env = env;
            translate(cdef);
        } finally {
            // note that recursive invocations of this method fail hard
            this.make = null;
            this.env = null;
        }

        return cdef;
    }

    /** Make an instanceof expression.
     *  @param lhs      The expression.
     *  @param type     The type to be tested.
     */

    JCInstanceOf makeTypeTest(JCExpression lhs, JCExpression type) {
        JCInstanceOf tree = make.TypeTest(lhs, type);
        tree.type = syms.booleanType;
        return tree;
    }

    /** Make an attributed binary expression (copied from Lower).
     *  @param optag    The operators tree tag.
     *  @param lhs      The operator's left argument.
     *  @param rhs      The operator's right argument.
     */
    JCBinary makeBinary(JCTree.Tag optag, JCExpression lhs, JCExpression rhs) {
        JCBinary tree = make.Binary(optag, lhs, rhs);
        tree.operator = operators.resolveBinary(tree, optag, lhs.type, rhs.type);
        tree.type = tree.operator.type.getReturnType();
        return tree;
    }

    /** Make an attributed unary expression.
     *  @param optag    The operators tree tag.
     *  @param arg      The operator's argument.
     */
    JCTree.JCUnary makeUnary(JCTree.Tag optag, JCExpression arg) {
        JCTree.JCUnary tree = make.Unary(optag, arg);
        tree.operator = operators.resolveUnary(tree, optag, arg.type);
        tree.type = tree.operator.type.getReturnType();
        return tree;
    }

    JCExpression convert(JCExpression expr, Type target) {
        JCExpression result = make.at(expr.pos()).TypeCast(make.Type(target), expr);
        result.type = target;
        return result;
    }

    abstract class BindingContext {
        abstract VarSymbol bindingDeclared(BindingSymbol varSymbol);
        abstract VarSymbol getBindingFor(BindingSymbol varSymbol);
        abstract List<JCStatement> bindingVars(int diagPos);
        abstract JCStatement decorateStatement(JCStatement stat);
        abstract JCExpression decorateExpression(JCExpression expr);
        abstract BindingContext pop();
        abstract boolean tryPrepend(BindingSymbol binding, JCVariableDecl var);
    }

    class BasicBindingContext extends BindingContext {
        Map<BindingSymbol, VarSymbol> hoistedVarMap;
        BindingContext parent;

        public BasicBindingContext() {
            this.parent = bindingContext;
            this.hoistedVarMap = new LinkedHashMap<>();
        }

        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            VarSymbol res = parent.bindingDeclared(varSymbol);
            if (res == null) {
                res = new VarSymbol(varSymbol.flags(), varSymbol.name, varSymbol.type, currentMethodSym);
                res.setTypeAttributes(varSymbol.getRawTypeAttributes());
                hoistedVarMap.put(varSymbol, res);
            }
            return res;
        }

        @Override
        VarSymbol getBindingFor(BindingSymbol varSymbol) {
            VarSymbol res = parent.getBindingFor(varSymbol);
            if (res != null) {
                return res;
            }
            return hoistedVarMap.entrySet().stream()
                    .filter(e -> e.getKey().isAliasFor(varSymbol))
                    .findFirst()
                    .map(e -> e.getValue()).orElse(null);
        }

        @Override
        List<JCStatement> bindingVars(int diagPos) {
            if (hoistedVarMap.isEmpty()) return List.nil();
            ListBuffer<JCStatement> stats = new ListBuffer<>();
            for (Entry<BindingSymbol, VarSymbol> e : hoistedVarMap.entrySet()) {
                JCVariableDecl decl = makeHoistedVarDecl(diagPos, e.getValue());
                if (!e.getKey().isPreserved() ||
                    !parent.tryPrepend(e.getKey(), decl)) {
                    stats.add(decl);
                }
            }
            return stats.toList();
        }

        @Override
        JCStatement decorateStatement(JCStatement stat) {
            //if (E instanceof T N) {
            //     //use N
            //}
            //=>
            //{
            //    T N;
            //    if ((let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp))) {
            //        //use N
            //    }
            //}
            List<JCStatement> stats = bindingVars(stat.pos);
            if (stats.nonEmpty()) {
                stat = make.at(stat.pos).Block(0, stats.append(stat));
            }
            return stat;
        }

        @Override
        JCExpression decorateExpression(JCExpression expr) {
            //E instanceof T N && /*use of N*/
            //=>
            //(let T N; (let T' N$temp = E; N$temp instanceof T && (N = (T) N$temp == (T) N$temp)) && /*use of N*/)
            for (VarSymbol vsym : hoistedVarMap.values()) {
                expr = make.at(expr.pos).LetExpr(makeHoistedVarDecl(expr.pos, vsym), expr).setType(expr.type);
            }
            return expr;
        }

        @Override
        BindingContext pop() {
            return bindingContext = parent;
        }

        @Override
        boolean tryPrepend(BindingSymbol binding, JCVariableDecl var) {
            return false;
        }

        private JCVariableDecl makeHoistedVarDecl(int pos, VarSymbol varSymbol) {
            return make.at(pos).VarDef(varSymbol, null);
        }
    }

    private class BindingDeclarationFenceBindingContext extends BasicBindingContext {

        @Override
        VarSymbol bindingDeclared(BindingSymbol varSymbol) {
            return null;
        }

    }

    /** Make an attributed tree representing a literal. This will be an
     *  Ident node in the case of boolean literals, a Literal node in all
     *  other cases.
     *  @param type       The literal's type.
     *  @param value      The literal's value.
     */
    JCExpression makeLit(Type type, Object value) {
        return make.Literal(type.getTag(), value).setType(type.constType(value));
    }

    /** Make an attributed tree representing null.
     */
    JCExpression makeNull() {
        return makeLit(syms.botType, null);
    }
}
