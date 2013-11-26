/*******************************************************************************
 * Copyright (c) 2007, 2012 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *     Holger Voormann - tests for bug 279029
 *     Jeremie Bresson - bug 389812, 390081, 249344
 *******************************************************************************/
package org.eclipse.mylyn.wikitext.tracwiki.core;

import java.io.IOException;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.eclipse.mylyn.wikitext.core.osgi.OsgiServiceLocator;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import org.eclipse.mylyn.wikitext.core.parser.markup.MarkupLanguageConfiguration;
import org.eclipse.mylyn.wikitext.tests.TestUtil;

/**
 * @author David Green
 */
public class TracWikiLanguageTest extends TestCase {

	private MarkupParser parser;

	private TracWikiLanguage markupLanguage;

	@Override
	public void setUp() {
		markupLanguage = new TracWikiLanguage();
		parser = new MarkupParser(markupLanguage);
	}

	public void testIsDetectingRawHyperlinks() {
		assertTrue(getMarkupLanguage().isDetectingRawHyperlinks());
	}

	protected TracWikiLanguage getMarkupLanguage() {
		return markupLanguage;
	}

	public void testDiscoverable() {
		MarkupLanguage language = OsgiServiceLocator.getApplicableInstance().getMarkupLanguage("TracWiki");
		assertNotNull(language);
		assertTrue(language instanceof TracWikiLanguage);
	}

	/**
	 * If a macro is not recognized, nothing should be substituted.
	 */
	public void testMacroNotRecognised() throws IOException {
		String html = parser.parseToHtml("there is [[NoSuchMacro]] a macro [[NoSuchMacro()]] in the [[NoSuchMacro(params, go=here)]] page");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>there is  a macro  in the  page</p></body>").matcher(html).find());
	}

	/**
	 * If a we give the image macro an incorrect number of parameters, nothing should be substituted.
	 */
	public void testImageMacroIncorrectParams() throws IOException {
		String html = parser.parseToHtml("there is a macro [[Image]] in the [[Image()]] page");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>there is a macro  in the  page</p></body>").matcher(html).find());
	}

	/**
	 * Simplest possible use of the image macro.
	 */
	public void testImageMacroBasic() throws IOException {
		String html = parser.parseToHtml("there is a macro [[Image(local_attachment.png)]] in the [[Image(http://www.example.com/external.png)]] page");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>there is a macro <img border=\"0\" src=\"local_attachment.png\"/> in the <img border=\"0\" src=\"http://www.example.com/external.png\"/> page</p></body>")
				.matcher(html)
				.find());
	}

	/**
	 * Image macro with various options set.
	 */
	public void testImageMacroOptions() throws IOException {
		String html = parser.parseToHtml("there is a macro [[Image(local_attachment.png, alt=Alt Text, title=Title Text, border=5)]] in the page");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>there is a macro <img alt=\"Alt Text\" title=\"Title Text\" border=\"5\" src=\"local_attachment.png\"/> in the page</p></body>")
				.matcher(html)
				.find());
	}

	/**
	 * Image macro with implicitly set width and height. Width may be specified without the "width=".
	 */
	public void testImageMacroSizes() throws IOException {
		String html = parser.parseToHtml("there is a macro [[Image(local_attachment.png, 100px, height=10%)]] in the page");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>there is a macro <img height=\"10%\" width=\"100\" border=\"0\" src=\"local_attachment.png\"/> in the page</p></body>")
				.matcher(html)
				.find());
	}

	/**
	 * Image macro with floating alignment, may be specified with or without the preceding "align=".
	 */
	public void testImageMacroFloatAlign() throws IOException {
		String html = parser.parseToHtml("there is a macro [[Image(local_attachment.png, right)]] in the [[Image(local_attachment.png, align=left)]] page");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>there is a macro <img style=\"float:right;\" border=\"0\" src=\"local_attachment.png\"/> in the <img style=\"float:left;\" border=\"0\" src=\"local_attachment.png\"/> page</p></body>")
				.matcher(html)
				.find());
	}

	/**
	 * Image macro ignores incorrectly formatted or unrecognized options.
	 */
	public void testImageMacroInvalidOptions() throws IOException {
		String html = parser.parseToHtml("there is a macro [[Image(local_attachment.png, beans, align=beans, border=b, width=10ee)]] in the page");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>there is a macro <img border=\"0\" src=\"local_attachment.png\"/> in the page</p></body>")
				.matcher(html)
				.find());
	}

	public void testParagraphs() throws IOException {
		String html = parser.parseToHtml("first para\nnew line\n\nsecond para\n\n\n\n");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>first para\\s*new line</p><p>second para</p></body>", Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testBoldItalic() {
		String html = parser.parseToHtml("normal '''''bold italic text''''' normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <b><i>bold italic text</i></b> normal</p></body>")
				.matcher(html)
				.find());
	}

	public void testBold() {
		String html = parser.parseToHtml("normal '''bold text''' normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <b>bold text</b> normal</p></body>").matcher(html).find());
	}

	public void testBoldEscaped() {
		String html = parser.parseToHtml("normal '''!'''bold text''' normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <b>'''bold text</b> normal</p></body>").matcher(html).find());
	}

	public void testItalic() {
		String html = parser.parseToHtml("normal ''italic text'' normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <i>italic text</i> normal</p></body>").matcher(html).find());
	}

	// test for bug 263015
	public void testItalic2() {
		String html = parser.parseToHtml("normal ''italic''-''italic'' normal");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>normal <i>italic</i>-<i>italic</i> normal</p></body>"));
	}

	public void testDeleted() {
		String html = parser.parseToHtml("normal ~~test text~~ normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <del>test text</del> normal</p></body>").matcher(html).find());
	}

	public void testDeleted2() {
		String html = parser.parseToHtml("normal ~~~test text~~ normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <del>~test text</del> normal</p></body>").matcher(html).find());
	}

	public void testDeleted3() {
		String html = parser.parseToHtml("normal ~~test text~~~ normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <del>test text</del>~ normal</p></body>").matcher(html).find());
	}

	public void testDeleted_AtStartOfLine() {
		String html = parser.parseToHtml("~~test text~~ normal");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p><del>test text</del> normal</p></body>"));
	}

	public void testDeleted_AtEndOfLine() {
		String html = parser.parseToHtml("normal ~~test text~~");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>normal <del>test text</del></p></body>"));
	}

	public void testUnderlined() {
		String html = parser.parseToHtml("normal __test text__ normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <u>test text</u> normal</p></body>").matcher(html).find());
	}

	public void testSuperscript() {
		String html = parser.parseToHtml("normal ^test text^ normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <sup>test text</sup> normal</p></body>").matcher(html).find());
	}

	public void testSubscript() {
		String html = parser.parseToHtml("normal ,,test text,, normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <sub>test text</sub> normal</p></body>").matcher(html).find());
	}

	public void testEscapedWithBacktick() {
		String html = parser.parseToHtml("normal `test text` normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <tt>test text</tt> normal</p></body>").matcher(html).find());
	}

	public void testEscapedWithCurlys() {
		String html = parser.parseToHtml("normal {{test text}} normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal <tt>test text</tt> normal</p></body>").matcher(html).find());
	}

	public void testHeadings() {
		for (int x = 1; x <= 6; ++x) {
			String delimiter = repeat(x, "=");
			String html = parser.parseToHtml(delimiter + "heading text" + delimiter
					+ "\nfirst para\nfirst para line2\n\nsecond para\n\nthird para");
			TestUtil.println(html);
			assertTrue(Pattern.compile(
					"<body><h" + x + " id=\"headingtext\">heading text</h" + x
							+ "><p>first para\\s*first para line2</p><p>second para</p><p>third para</p></body>",
					Pattern.MULTILINE)
					.matcher(html)
					.find());

			html = parser.parseToHtml(delimiter + "heading text" + delimiter + " #with-id-" + x
					+ "\nfirst para\nfirst para line2\n\nsecond para\n\nthird para");
			TestUtil.println(html);
			assertTrue(Pattern.compile(
					"<body><h" + x + " id=\"with-id-" + x + "\">heading text</h" + x
							+ "><p>first para\\s*first para line2</p><p>second para</p><p>third para</p></body>",
					Pattern.MULTILINE)
					.matcher(html)
					.find());

			html = parser.parseToHtml(delimiter + "heading text" + delimiter + "    \n"
					+ "first para\nfirst para line2\n\nsecond para\n\nthird para");
			TestUtil.println(html);
			assertTrue(Pattern.compile(
					"<body><h" + x + " id=\"headingtext\">heading text</h" + x
							+ "><p>first para\\s*first para line2</p><p>second para</p><p>third para</p></body>",
					Pattern.MULTILINE)
					.matcher(html)
					.find());
		}
	}

	public void testHeadingBreakingPara() {
		String html = parser.parseToHtml("=\n== heading ==\npara");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>=</p><h2 id=\"heading\">heading</h2><p>para</p></body>"));
	}

	private String repeat(int i, String string) {
		StringBuilder buf = new StringBuilder(string.length() * i);
		for (int x = 0; x < i; ++x) {
			buf.append(string);
		}
		return buf.toString();
	}

	public void testLineBreak() {
		String html = parser.parseToHtml("normal text[[BR]]normal");
		TestUtil.println(html);
		assertTrue(Pattern.compile("<body><p>normal text<br/>\\s*normal</p></body>").matcher(html).find());
	}

	public void testListUnordered() throws IOException {
		String html = parser.parseToHtml(" * a list\n * with two lines");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<body><ul><li>a list</li><li>with two lines</li></ul></body>"));
	}

	public void testListUnorderedWithoutSpacePrefix() throws Exception {
		//bug 389812
		String html = parser.parseToHtml("* a list\n* with two lines");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<body><ul><li>a list</li><li>with two lines</li></ul></body>"));
	}

	public void testListUnorderedBigSpacePrefix() throws Exception {
		//bug 389812
		String html = parser.parseToHtml("   * a list\n   * with two lines");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<body><ul><li>a list</li><li>with two lines</li></ul></body>"));
	}

	public void testListUnorderedWithHyphens() throws IOException {
		String html = parser.parseToHtml(" - a list\n - with two lines");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<body><ul><li>a list</li><li>with two lines</li></ul></body>"));
	}

	public void testListOrdered() throws IOException {
		String html = parser.parseToHtml(" 1. a list\n 2. with two lines");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<ol>"));
		assertTrue(html.contains("<li>a list</li>"));
		assertTrue(html.contains("<li>with two lines</li>"));
		assertTrue(html.contains("</ol>"));
	}

	public void testListOrderedWithoutSpacePrefix() throws Exception {
		//bug 389812
		String html = parser.parseToHtml("1. a list\n1. with two lines");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<body><ol><li>a list</li><li>with two lines</li></ol></body>"));
	}

	public void testListOrderedStartAt2() throws IOException {
		String html = parser.parseToHtml(" 2. with two lines\n 3. three");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<body><ol start=\"2\"><li>with two lines</li><li>three</li></ol></body>"));
	}

	public void testListOrderedBug265015() throws IOException {
		String html = parser.parseToHtml(" 1. first\n\n 2. second");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<body><ol><li>first</li></ol><ol start=\"2\"><li>second</li></ol></body>"));
	}

	public void testListNested() throws IOException {
		String html = parser.parseToHtml(" 1. a list\n  1. nested\n  1. nested2\n 1. level1\n\npara");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<ol>"));
		assertTrue(html.contains("<li>a list"));
		assertTrue(html.contains("<li>nested"));
		assertTrue(html.contains("</ol>"));
	}

	public void testListNestedUnordered() throws Exception {
		//bug 389812
		String html = parser.parseToHtml("* Apples\n  * Sauce\n  * Juice\n* Oranges\n* Grapes");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<ul><li>Apples<ul><li>Sauce</li><li>Juice</li></ul></li><li>Oranges</li><li>Grapes</li></ul>"));
	}

	public void testListNestedMixed() throws IOException {
		String html = parser.parseToHtml(" 1. a list\n  * nested\n  * nested2\n 1. level1\n\npara");

		TestUtil.println("HTML: \n" + html);
		assertTrue(html.contains("<ol><li>a list<ul><li>nested</li><li>nested2</li></ul></li><li>level1</li></ol>"));
	}

	public void testListNumericWithBulleted() {
		String html = parser.parseToHtml("   1. one\n   * two");
		TestUtil.println(html);
		assertTrue(html.contains("<body><ol><li>one</li></ol><ul><li>two</li></ul></body>"));
	}

	public void testListMultipleLines() throws Exception {
		//390081
		String html = parser.parseToHtml(" * an item\n   with 2 lines\n * other\n second line\n * ok?\nAnd a new paragraph");
		TestUtil.println(html);
		assertTrue(html.contains("<body><ul><li>an item with 2 lines</li><li>other second line</li><li>ok?</li></ul><p>And a new paragraph</p></body>"));
	}

	public void testListMultipleLinesWithoutSpacePrefix() throws Exception {
		//390081
		String html = parser.parseToHtml("- an item\ncreate paragraph\n- try\n this\n again\n- it\n  is working\n- ok?");
		TestUtil.println(html);
		assertTrue(html.contains("<body><ul><li>an item</li></ul><p>create paragraph</p><ul><li>try this again</li><li>it is working</li><li>ok?</li></ul></body>"));
	}

	public void testListMultipleLinesBigSpacePrefix() throws Exception {
		//390081
		String html = parser.parseToHtml("    1. item1\n       more\n       lines\n    1. item2");
		TestUtil.println(html);
		assertTrue(html.contains("<body><ol><li>item1 more lines</li><li>item2</li></ol></body>"));
	}

	public void testListMultipleLinesQuote() throws Exception {
		//390081
		String html = parser.parseToHtml("1. item1\n    wrong: create a quote\n1. item2");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><ol><li>item1</li></ol><blockquote><p>\\s*wrong: create a quote</p></blockquote><ol><li>item2</li></ol></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testDefinitionList() throws Exception {
		// bug 249344
		String html = parser.parseToHtml(" item1:: foo.\n item2:: bar.\n");
		TestUtil.println(html);

		assertTrue(html.contains("<body><dl><dt>item1</dt><dd>foo.</dd><dt>item2</dt><dd>bar.</dd></dl></body>"));
	}

	public void testDefinitionListMultiline() throws Exception {
		// bug 249344
		String html = parser.parseToHtml(" first important term:: this\n                        is important\n second term::\n is not important.");
		TestUtil.println(html);

		assertTrue(html.contains("<body><dl><dt>first important term</dt><dd>this is important</dd><dt>second term</dt><dd>is not important.</dd></dl></body>"));
	}

	public void testDefinitionListWithoutSpacePrefix() throws Exception {
		// bug 249344
		String html = parser.parseToHtml("this:: is\nnot a list\nspace::\nis required.");
		TestUtil.println(html);

		assertFalse(html.contains("<dl>"));
		assertFalse(html.contains("<dt>"));
		assertFalse(html.contains("<dd>"));
	}

	public void testDefinitionListWithBigSpace() throws Exception {
		// bug 249344
		String html = parser.parseToHtml("    lorem:: ipsum\n           dolore\n and enough indentation\n    remlo::      relodo");
		TestUtil.println(html);

		assertTrue(html.contains("<body><dl><dt>lorem</dt><dd>ipsum dolore and enough indentation</dd><dt>remlo</dt><dd>relodo</dd></dl></body>"));
	}

	public void testDefinitionListAndParagraph() throws Exception {
		// bug 249344
		String html = parser.parseToHtml(" a:: 1\n 2\nparagraph\n\n b::\n 3\n \n 4\n\n x");
		TestUtil.println(html);

		assertTrue(html.contains("<dl><dt>a</dt><dd>1 2</dd></dl>"));
		assertTrue(html.contains("<p>paragraph</p>"));
		assertTrue(Pattern.compile("<dl><dt>b</dt><dd>3\\s+4</dd></dl>", Pattern.MULTILINE).matcher(html).find());
	}

	public void testPreformatted() throws IOException {
		String html = parser.parseToHtml("first para\n\n{{{\n\tpreformatted text\n\nspanning multilple lines\n}}}\nsecond para");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>first para</p><pre>\\s*?\tpreformatted text\\s*spanning multilple lines\\s*</pre><p>second para</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testPreformattedNextToPara() throws IOException {
		String html = parser.parseToHtml("first para\n{{{\n\tpreformatted text\n\nspanning multilple lines\n}}}\nsecond para");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>first para</p><pre>\\s*?\tpreformatted text\\s*spanning multilple lines\\s*</pre><p>second para</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testPreformattedInline() throws IOException {
		String html = parser.parseToHtml("first para {{{ preformatted text }}} more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>first para <tt> preformatted text </tt> more text</p></body>"));
	}

	public void testPreformattedInline2() throws IOException {
		String html = parser.parseToHtml("first para {{{ preformatted text }}} and {{{ more code }}} more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>first para <tt> preformatted text </tt> and <tt> more code </tt> more text</p></body>"));
	}

	public void testPreformattedInline3() throws IOException {
		String html = parser.parseToHtml("{{{ preformatted text }}}");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p><tt> preformatted text </tt></p></body>"));
	}

	public void testQuoteBlock() throws IOException {
		String html = parser.parseToHtml("" + ">> second level\n" + ">> second level line 2\n" + "> first level\n"
				+ "new para\n" + "");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><blockquote><blockquote><p>second level<br/>\\s*second level line 2</p></blockquote><p>first level</p></blockquote><p>new para</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testQuoteBlockFollowingPara() throws IOException {
		String html = parser.parseToHtml("" + "normal para\n" + "> quoted\n" + "new para\n" + "");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>normal para</p><blockquote><p>quoted</p></blockquote><p>new para</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testQuoteBlockWithSpaces() throws IOException {
		String html = parser.parseToHtml("" + "normal para\n" + "  quoted\n" + "  first level\n" + "new para\n" + "");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>normal para</p><blockquote><p>quoted<br/>\\s*first level</p></blockquote><p>new para</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testTableBlock() {
		String html = parser.parseToHtml("" + "normal para\n" + "||a table||row with three||columns||\n"
				+ "||another||row||||\n" + "new para\n" + "");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body>" + "<p>normal para</p>" + "<table>" + "<tr>" + "<td>a table</td>" + "<td>row with three</td>"
						+ "<td>columns</td>" + "</tr>" + "<tr>" + "<td>another</td>" + "<td>row</td>" + "<td></td>"
						+ "</tr>" + "</table>" + "<p>new para</p></body>", Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testHyperlink() {
		String html = parser.parseToHtml("a normal para http://www.example.com with a hyperlink");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>a normal para <a href=\"http://www.example.com\">http://www.example.com</a> with a hyperlink</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testHyperlinkWithTitle() {
		String html = parser.parseToHtml("a normal para [http://www.example.com Example ] with a hyperlink");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>a normal para <a href=\"http://www.example.com\">Example</a> with a hyperlink</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testHyperlinkWithoutTitle() {
		String html = parser.parseToHtml("a normal para [http://www.example.com] with a hyperlink");
		TestUtil.println(html);
		assertTrue(Pattern.compile(
				"<body><p>a normal para <a href=\"http://www.example.com\">http://www.example.com</a> with a hyperlink</p></body>",
				Pattern.MULTILINE)
				.matcher(html)
				.find());
	}

	public void testInternalHyperlinkWithTitle() {
		String html = parser.parseToHtml("a normal para [wiki:ISO9000 ISO 9000] with a hyperlink");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>a normal para <a href=\"ISO9000\">ISO 9000</a> with a hyperlink</p></body>"));
	}

	public void testInternalHyperlinkWithoutTitle() {
		String html = parser.parseToHtml("a normal para [wiki:ISO9000] with a hyperlink");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>a normal para <a href=\"ISO9000\">ISO9000</a> with a hyperlink</p></body>"));
	}

	public void testWikiWord() {
		markupLanguage.setInternalLinkPattern("https://foo.bar/wiki/{0}");
		String html = parser.parseToHtml("A WikiWord points somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A <a href=\"https://foo.bar/wiki/WikiWord\">WikiWord</a> points somewhere</p></body>"));
	}

	public void testWikiWordDisabled() {
		MarkupLanguageConfiguration configuration = new MarkupLanguageConfiguration();
		configuration.setWikiWordLinking(false);
		markupLanguage.configure(configuration);
		markupLanguage.setInternalLinkPattern("https://foo.bar/wiki/{0}");
		String html = parser.parseToHtml("A WikiWord points somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A WikiWord points somewhere</p></body>"));
	}

	public void testWikiColon() {
		markupLanguage.setInternalLinkPattern("https://foo.bar/wiki/{0}");
		String html = parser.parseToHtml("A wiki:word points somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A <a href=\"https://foo.bar/wiki/word\">word</a> points somewhere</p></body>"));
	}

	public void testWikiWordNegativeMatch() {
		testWikiWordNegativeMatch("A noWikiWord points somewhere");
		testWikiWordNegativeMatch("A noAWikiWord points somewhere");
		testWikiWordNegativeMatch("A aBBaB points somewhere");
		testWikiWordNegativeMatch("A XML HTML or PDF points NOT somewhere");
		testWikiWordNegativeMatch("not a WikiWOrd");
		testWikiWordNegativeMatch("not a 1WikiWord");
		testWikiWordNegativeMatch("1WikiWord WIkiWord O2WikiWord");
		testWikiWordNegativeMatch("Wiki-Word Wi-kiWord");
		testWikiWordNegativeMatch("not a WikiWord", "not a !WikiWord");
	}

	private void testWikiWordNegativeMatch(String toTest) {
		testWikiWordNegativeMatch(toTest, toTest);
	}

	private void testWikiWordNegativeMatch(String expected, String toTest) {
		markupLanguage.setInternalLinkPattern("https://foo.bar/wiki/{0}");
		String html = parser.parseToHtml(toTest);
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>" + expected + "</p></body>"));
	}

	public void testWikiWordAtLineStart() {
		markupLanguage.setInternalLinkPattern("https://foo.bar/wiki/{0}");
		String html = parser.parseToHtml("WikiWord points somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p><a href=\"https://foo.bar/wiki/WikiWord\">WikiWord</a> points somewhere</p></body>"));
	}

	public void testWikiWordAtLineEnd() {
		markupLanguage.setInternalLinkPattern("https://foo.bar/wiki/{0}");
		String html = parser.parseToHtml("a WikiWord");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>a <a href=\"https://foo.bar/wiki/WikiWord\">WikiWord</a></p></body>"));
	}

// BBaB is not a WikiWord because of the two sequenced upper-case letters
//	public void testWikiWord2() {
//		markupLanaguage.setInternalLinkPattern("https://foo.bar/wiki/{0}");
//		String html = parser.parseToHtml("a BBaB");
//		TestUtil.println(html);
//		assertTrue(html.contains("<body><p>a <a href=\"https://foo.bar/wiki/BBaB\">BBaB</a></p></body>"));
//	}

	public void testWikiWordNoAutolink() {
		markupLanguage.setAutoLinking(false);
		String html = parser.parseToHtml("A WikiWord points somewhere but not this one!");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A WikiWord points somewhere but not this one!</p></body>"));
	}

	public void testWikiWordEscaped() {
		String html = parser.parseToHtml("A !WikiWord points somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A WikiWord points somewhere</p></body>"));
	}

	public void testTicketLink() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A ticket #1 or ticket:1 to somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A ticket <a href=\"http://trac.edgewall.org/ticket/1\">#1</a> or <a href=\"http://trac.edgewall.org/ticket/1\">ticket:1</a> to somewhere</p></body>"));
	}

	public void testTicketLinkNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A ticket a#1 or aticket:1 to somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A ticket a#1 or aticket:1 to somewhere</p></body>"));
	}

	public void testTicketLinkWithComment() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A ticket comment:1:ticket:2 to somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A ticket <a href=\"http://trac.edgewall.org/ticket/2#comment:1\">comment:1:ticket:2</a> to somewhere</p></body>"));
	}

	public void testTicketLinkWithCommentNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A ticket acomment:1:ticket:2 to somewhere");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A ticket acomment:1:<a href=\"http://trac.edgewall.org/ticket/2\">ticket:2</a> to somewhere</p></body>"));
	}

	public void testReportLink() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A report:1 about something");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A <a href=\"http://trac.edgewall.org/report/1\">report:1</a> about something</p></body>"));
	}

	public void testReportLinkNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A areport:1 about something");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A areport:1 about something</p></body>"));
	}

	public void testChangesetLink() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A changeset r1 or [1] or [1/trunk] or changeset:1 or changeset:1/trunk more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A changeset <a href=\"http://trac.edgewall.org/changeset/1\">r1</a> or <a href=\"http://trac.edgewall.org/changeset/1\">[1]</a> or <a href=\"http://trac.edgewall.org/changeset/1/trunk\">[1/trunk]</a> or <a href=\"http://trac.edgewall.org/changeset/1\">changeset:1</a> or <a href=\"http://trac.edgewall.org/changeset/1/trunk\">changeset:1/trunk</a> more text</p></body>"));
	}

	public void testChangesetLinkNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A changeset ar1 or a[1] or a[1/trunk] or achangeset:1 or achangeset:1/trunk more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A changeset ar1 or a[1] or a[1/trunk] or achangeset:1 or achangeset:1/trunk more text</p></body>"));
	}

	public void testRevisionLogLink() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A revision log r1:3, [1:3] or log:@1:3, log:trunk@1:3, [2:5/trunk] more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A revision log <a href=\"http://trac.edgewall.org/log/?revs=1-3\">r1:3</a>, <a href=\"http://trac.edgewall.org/log/?revs=1-3\">[1:3]</a> or <a href=\"http://trac.edgewall.org/log/?revs=1-3\">log:@1:3</a>, <a href=\"http://trac.edgewall.org/log/trunk?revs=1-3\">log:trunk@1:3</a>, <a href=\"http://trac.edgewall.org/log/trunk?revs=2-5\">[2:5/trunk]</a> more text</p></body>"));
	}

	public void testRevisionLogLinkNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A revision log ar1:3, a[1:3] or alog:@1:3, alog:trunk@1:3, a[2:5/trunk] more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A revision log ar1:3, a[1:3] or alog:@1:3, alog:trunk@1:3, a[2:5/trunk] more text</p></body>"));
	}

	public void testMilestoneLink() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A milestone:1.0 more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A <a href=\"http://trac.edgewall.org/milestone/1.0\">milestone:1.0</a> more text</p></body>"));
	}

	public void testMilestoneLinkNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A amilestone:1.0 more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A amilestone:1.0 more text</p></body>"));
	}

	public void testTicketAttachmentLink() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A attachment:foobar.txt:ticket:12345 more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A <a href=\"http://trac.edgewall.org/ticket/12345/foobar.txt\">attachment:foobar.txt:ticket:12345</a> more text</p></body>"));
	}

	public void testTicketAttachmentLinkNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A Aattachment:foobar.txt:ticket:12345 more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A Aattachment:foobar.txt:<a href=\"http://trac.edgewall.org/ticket/12345\">ticket:12345</a> more text</p></body>"));
	}

	public void testSourceLink() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A source:/trunk/COPYING or source:/trunk/COPYING@200 or source:/trunk/COPYING@200#L26 more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A <a href=\"http://trac.edgewall.org/browser/trunk/COPYING\">source:/trunk/COPYING</a> or <a href=\"http://trac.edgewall.org/browser/trunk/COPYING?rev=200\">source:/trunk/COPYING@200</a> or <a href=\"http://trac.edgewall.org/browser/trunk/COPYING?rev=200#L26\">source:/trunk/COPYING@200#L26</a> more text</p></body>"));
	}

	public void testSourceLinkNegativeMatch() {
		markupLanguage.setServerUrl("http://trac.edgewall.org");
		String html = parser.parseToHtml("A Asource:/trunk/COPYING or Asource:/trunk/COPYING@200 or Asource:/trunk/COPYING@200#L26 more text");
		TestUtil.println(html);
		assertTrue(html.contains("<body><p>A Asource:/trunk/COPYING or Asource:/trunk/COPYING@200 or Asource:/trunk/COPYING@200#L26 more text</p></body>"));
	}

}
