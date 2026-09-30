package com.rag.ingestion.common;

import com.rag.ingestion.common.model.Section;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.springframework.stereotype.Component;

/**
 * Splits the HTML of a page into one {@link Section} per heading, keeping one line per block element.
 *
 * <p>Each section is embedded on its own, so a chunk covers a single topic and the splitter can cut long
 * sections at line breaks instead of in the middle of a paragraph.
 */
@Component
public class HtmlSectionSplitter {

  private static final Set<String> HEADINGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

  /** Returns the sections of the page in document order, without the empty ones. */
  public List<Section> split(String html) {
    var visitor = new SectionVisitor();
    NodeTraversor.traverse(visitor, Jsoup.parseBodyFragment(html).body());
    return visitor.sections();
  }

  /** Walks the DOM so headings nested in layout macros or tables still start a section. */
  private static final class SectionVisitor implements NodeVisitor {

    private final List<Section> sections = new ArrayList<>();
    private final StringBuilder text = new StringBuilder();
    private String heading = "";
    private int headingDepth = -1;

    @Override
    public void head(Node node, int depth) {
      if (node instanceof Element element && HEADINGS.contains(element.normalName())) {
        flush();
        heading = element.text().trim();
        headingDepth = depth;
      } else if (node instanceof TextNode textNode && headingDepth < 0) {
        text.append(textNode.text());
      }
    }

    @Override
    public void tail(Node node, int depth) {
      if (depth == headingDepth) {
        headingDepth = -1;
      } else if (node instanceof Element element && (element.isBlock() || element.nameIs("br"))) {
        text.append('\n');
      }
    }

    List<Section> sections() {
      flush();
      return List.copyOf(sections);
    }

    private void flush() {
      String body =
          text.toString().lines().map(String::strip).filter(line -> !line.isEmpty()).reduce((a, b) -> a + "\n" + b).orElse("");
      if (!body.isEmpty()) {
        sections.add(new Section(heading, body));
      }
      text.setLength(0);
    }
  }
}
