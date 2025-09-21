package com.example.transparentclass.inlay;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class InheritedMembersInlayProvider implements InlayHintsProvider<NoSettings> {

    @NotNull
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file, @NotNull Editor editor, @NotNull NoSettings settings, @NotNull InlayHintsSink sink) {
        return (element, editor1, sink1) -> {
            if (element instanceof PsiClass psiClass) {
                PsiElement lBraceElement = psiClass.getLBrace();
                if (!(lBraceElement instanceof PsiJavaToken)) return true;

                PresentationFactory factory = new PresentationFactory(editor1);
                PsiClass superClass = psiClass.getSuperClass();
                if (superClass == null || "Object".equals(superClass.getName())) return true;

                //fields
                Set<String> currentFieldNames = Arrays.stream(psiClass.getFields()).map(PsiField::getName).collect(Collectors.toSet());
                for (PsiField field : superClass.getFields()) {
                    if (!field.getModifierList().hasModifierProperty(PsiModifier.PRIVATE) && !currentFieldNames.contains(field.getName())) {
                        String hintText = String.format("↑ %s %s", field.getType().getPresentableText(), field.getName());
                        sink1.addBlockElement(lBraceElement.getTextOffset() + 1, true, true, 0, factory.text(hintText));
                    }
                }

                // method
                Set<String> currentMethodNames = Arrays.stream(psiClass.getMethods()).map(PsiMethod::getName).collect(Collectors.toSet());
                Set<String> processedSignatures = new HashSet<>();
                for (PsiMethod method : superClass.getMethods()) {
                    if (!method.getModifierList().hasModifierProperty(PsiModifier.PRIVATE) && !method.isConstructor() && !currentMethodNames.contains(method.getName())) {
                        String signature = getMethodSignature(method);
                        if (processedSignatures.add(signature)) {
                            String hintText = "↑ " + buildHintText(method);
                            sink1.addBlockElement(lBraceElement.getTextOffset() + 1, true, true, 0, factory.text(hintText));
                        }
                    }
                }
            }
            return true;
        };
    }

    private String buildHintText(PsiMethod method) {
        String returnType = method.getReturnType() != null ? method.getReturnType().getPresentableText() : "void";
        String params = Arrays.stream(method.getParameterList().getParameters()).map(p -> p.getType().getPresentableText()).collect(Collectors.joining(", "));
        return String.format("%s %s(%s)", returnType, method.getName(), params);
    }
    private String getMethodSignature(PsiMethod method) {
        String params = Arrays.stream(method.getParameterList().getParameters()).map(p -> p.getType().getCanonicalText()).collect(Collectors.joining(","));
        return method.getName() + "(" + params + ")";
    }

    @NotNull @Override public String getName() { return "Inherited Members"; }
    @NotNull @Override public SettingsKey<NoSettings> getKey() { return new SettingsKey<>("java.inherited.members"); }
    @NotNull @Override public NoSettings createSettings() { return new NoSettings(); }
    @Nullable @Override public String getPreviewText() { return null; }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull NoSettings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener listener) {
                return new JPanel();
            }
        };
    }
}