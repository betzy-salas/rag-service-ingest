package com.rag.ingestion.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.rag.ingestion.common.model.Section;
import org.junit.jupiter.api.Test;

class HtmlSectionSplitterTest {

  private final HtmlSectionSplitter splitter = new HtmlSectionSplitter();

  @Test
  void splitsOneSectionPerHeadingWithTheTextBeforeTheFirstOne() {
    var html =
        "<p>Intro.</p><h2>Rollback</h2><p>Revierte primero.</p><p>Investiga después.</p>"
            + "<h2>CI/CD</h2><p>GitHub Actions.</p>";

    assertThat(splitter.split(html))
        .containsExactly(
            new Section("", "Intro."),
            new Section("Rollback", "Revierte primero.\nInvestiga después."),
            new Section("CI/CD", "GitHub Actions."));
  }

  @Test
  void findsHeadingsNestedInLayoutMacrosAndKeepsOneLinePerListItem() {
    var html =
        "<ac:layout><ac:layout-cell><h1>Guía</h1><ul><li>Paso <b>uno</b></li><li>Paso dos</li></ul>"
            + "</ac:layout-cell></ac:layout>";

    assertThat(splitter.split(html)).containsExactly(new Section("Guía", "Paso uno\nPaso dos"));
  }

  @Test
  void ignoresHeadingsWithoutTextAndPagesWithoutHeadings() {
    assertThat(splitter.split("<h2>Vacío</h2><h2>Solo</h2><p>Texto.</p>"))
        .containsExactly(new Section("Solo", "Texto."));
    assertThat(splitter.split("<p>Sin encabezado.</p>")).containsExactly(new Section("", "Sin encabezado."));
    assertThat(splitter.split("<h2>Nada</h2>")).isEmpty();
  }
}
