package com.github.gfjalar.intellij.wordspec.plugin;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;

import java.util.*;

public class WordSpecExpression {
  private final static Set<String> WORD_SPEC_NODES = new HashSet<String>(
    Arrays.asList("when", "should", "must", "can", "which")
  );
  private final static Set<String> WORD_SPEC_LEAVES = new HashSet<String>(
    Arrays.asList("in")
  );

  public final PsiElement element;
  public final String subject;
  public final String word;
  public final boolean isNode;
  public final boolean isLeaf;

  public WordSpecExpression(PsiElement element) {
    this.element = element;

    assert element.toString() == "InfixExpression";
    assert element.getChildren().length > 1;
    assert element.getFirstChild().toString() == "StringLiteral";
    assert element.getLastChild().toString() == "BlockExpression";
    assert element.getChildren()[1].toString().startsWith("ReferenceExpression");

    this.subject = element.getFirstChild().getText().substring(1, element.getFirstChild().getTextLength() - 1);
    this.word = element.getChildren()[1].getText();
    this.isNode = WORD_SPEC_NODES.contains(word);
    this.isLeaf = WORD_SPEC_LEAVES.contains(word);

    assert isNode ^ isLeaf;
  }

  public static WordSpecExpression apply(PsiElement element) {
    try {
      return new WordSpecExpression(element);
    } catch(AssertionError e) {
      return null;
    }
  }

  private static WordSpecExpression fromLeafIdentifier(PsiElement element) {
    if (! (element instanceof LeafPsiElement)) { return null; }
    if (! ((LeafPsiElement) element).getElementType().toString().equals("identifier")) { return null; }
    if (element.getParent() == null || element.getParent().getParent() == null) { return null; }
    return apply(element.getParent().getParent());
  }

  public static WordSpecExpression fromLeafStringContent(PsiElement element) {
    if (! (element instanceof LeafPsiElement)) { return null; }
    if (element.getText().contains("abc")) { System.out.println(((LeafPsiElement) element).getElementType().toString()); }
    if (! ((LeafPsiElement) element).getElementType().toString().equals("string content")) { return null; }
    if (element.getParent() == null || element.getParent().getParent() == null) { return null; }
    if (element.getParent().getParent().getFirstChild() != element.getParent()) { return null; }
    return apply(element.getParent().getParent());
  }

  public String toString() {
    if (isLeaf) {
      return subject;
    } else {
      return subject + " " + word;
    }
  }

  public String toSentence() {
    List<String> words = new ArrayList<String>();

    if (isNode) { words.add("*"); }
    words.add(toString());

    PsiElement next = element;
    while (next.getParent() != null) {
      next = next.getParent();
      WordSpecExpression expression = WordSpecExpression.apply(next);
      if (expression != null) {
        words.add(expression.toString());
      }
    }
    Collections.reverse(words);
    return String.join(" ", words);
  }
}