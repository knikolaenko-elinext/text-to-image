CREATE OR REPLACE AND COMPILE JAVA SOURCE NAMED "TextRenderServiceImpl" AS
package by.knick.eli;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import oracle.sql.BLOB;

public class TextRenderServiceImpl {
	private static final int PREFFERABLE_WIDTH = 198;
	private static final int PREFFERABLE_HEIGHT_SHORT = 68;
	private static final int PREFFERABLE_HEIGHT_TALL = 198;

	private static final int MINIMAL_FONT_SIZE = 10;
	private static final int SIGNATURE_FONT_SIZE = MINIMAL_FONT_SIZE;
	private static final int SIGNATURE_PADDING = 5;
	private static final int MAXIMUM_FONT_SIZE = PREFFERABLE_HEIGHT_SHORT - SIGNATURE_FONT_SIZE - SIGNATURE_PADDING;

	private static final String FONT_FAMILY = "HelveticaNeueLT Com 57 Cn";
	private static final Color FONT_COLOR = Color.BLACK;
	private static final Color BACKGROUND_COLOR = new Color(0xDD, 0xDD, 0xDD);
	private static final int SIDE_PADDING = 1;

	private static final double HIT_RATE_THRESHOLD = 0.95;
	private static final int MAX_ITERATIONS = 20;

	private static final String SIGNATURE_TEXT = "\u00A9 mmi GmbH";

	public byte[] renderText(String text, String imgFormatName) {
		SizeCalculationResult sizeCalculationResult = calculateFontSize(text);

		float fontSize = sizeCalculationResult.fontSize;
		boolean useTallVersion = sizeCalculationResult.useTallVersion;

		Hashtable<TextAttribute, Object> textAttributes = new Hashtable<TextAttribute, Object>();
		textAttributes.put(TextAttribute.FAMILY, FONT_FAMILY);
		textAttributes.put(TextAttribute.SIZE, new Float(fontSize));
		AttributedString attributedText = new AttributedString(text, textAttributes);

		// Create a new LineBreakMeasurer from the paragraph.
		AttributedCharacterIterator paragraph = attributedText.getIterator();
		// index of the first character in the paragraph.
		int paragraphStart = paragraph.getBeginIndex();
		// index of the first character after the end of the paragraph.
		int paragraphEnd = paragraph.getEndIndex();

		int imgHeight = useTallVersion ? PREFFERABLE_HEIGHT_TALL : PREFFERABLE_HEIGHT_SHORT;
		BufferedImage img = new BufferedImage(PREFFERABLE_WIDTH, imgHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();
		clearImage(useTallVersion, g2d);
		setRenderingHints(g2d);
		FontRenderContext frc = g2d.getFontRenderContext();

		LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);

		// Set break width to width of Component.
		float breakWidth = PREFFERABLE_WIDTH - 2 * SIDE_PADDING;
		float drawPosY = 0;
		// Set position to the index of the first character in the paragraph.
		lineMeasurer.setPosition(paragraphStart);

		// Get lines until the entire paragraph has been displayed.
		while (lineMeasurer.getPosition() < paragraphEnd) {

			// Retrieve next layout.
			TextLayout layout = lineMeasurer.nextLayout(breakWidth);

			// Compute pen x position.
			float drawPosX = SIDE_PADDING;

			// Move y-coordinate by the ascent of the layout.
			drawPosY += layout.getAscent();

			if (drawPosY + layout.getDescent() > imgHeight - SIGNATURE_FONT_SIZE - SIGNATURE_PADDING) {
				// Do not overlap signature
				break;
			}

			// Draw the TextLayout at (drawPosX, drawPosY).
			layout.draw(g2d, drawPosX, drawPosY);

			// Move y-coordinate in preparation for next layout.
			drawPosY += layout.getDescent() + layout.getLeading();
		}

		drawSignature(g2d, imgHeight);

		g2d.dispose();

		// Write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, imgFormatName, baos);
			return baos.toByteArray();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			try {
				baos.close();
			} catch (IOException e) {
			}
		}
	}

	private void drawSignature(Graphics2D g2d, int imgHeight) {
		Hashtable<TextAttribute, Object> textAttributes = new Hashtable<TextAttribute, Object>();
		textAttributes.put(TextAttribute.FAMILY, FONT_FAMILY);
		textAttributes.put(TextAttribute.SIZE, new Float(SIGNATURE_FONT_SIZE));
		Font font = new Font(textAttributes);
		g2d.setFont(font);

		float drawPosX = 1;
		float drawPosY = imgHeight - g2d.getFontMetrics().getDescent();
		g2d.drawString(SIGNATURE_TEXT, drawPosX, drawPosY);
	}

	private static final class SizeCalculationResult {
		float fontSize;
		boolean useTallVersion;

		public SizeCalculationResult(float fontSize, boolean useTallVersion) {
			super();
			this.fontSize = fontSize;
			this.useTallVersion = useTallVersion;
		}
	}

	SizeCalculationResult calculateFontSize(String text) {
		BufferedImage img = new BufferedImage(PREFFERABLE_WIDTH, PREFFERABLE_HEIGHT_TALL, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();
		setRenderingHints(g2d);
		FontRenderContext frc = g2d.getFontRenderContext();

		float currentFontSize = MINIMAL_FONT_SIZE;
		float prevIterationFontSize = currentFontSize;

		Hashtable<TextAttribute, Object> textAttributes = new Hashtable<TextAttribute, Object>();
		textAttributes.put(TextAttribute.FAMILY, FONT_FAMILY);

		boolean fontSizeFound = false;
		boolean useTallVersion = false;
		boolean didDecreasingOnLastIteration = false;
		int multiplyFactor = 2;
		int iterationNumber = 0;

		while (!fontSizeFound) {
			iterationNumber++;
			textAttributes.put(TextAttribute.SIZE, new Float(currentFontSize));

			AttributedString attributedText = new AttributedString(text, textAttributes);

			AttributedCharacterIterator paragraph = attributedText.getIterator();
			// Create a new LineBreakMeasurer from the paragraph.
			LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);
			// Set position to the index of the first character in the
			// paragraph.
			lineMeasurer.setPosition(paragraph.getBeginIndex());

			float breakWidth = PREFFERABLE_WIDTH - 2 * SIDE_PADDING;
			float drawPosY = 0;
			// Get lines until the entire paragraph has been displayed.
			while (lineMeasurer.getPosition() < paragraph.getEndIndex()) {
				// Retrieve next row layout
				TextLayout layout = lineMeasurer.nextLayout(breakWidth);
				// Move y-coordinate by the font size
				drawPosY += layout.getAscent() + layout.getDescent() + layout.getLeading();
			}

			if (!useTallVersion) {
				float hitRateForShortVersion = drawPosY / (PREFFERABLE_HEIGHT_SHORT - SIGNATURE_FONT_SIZE - SIGNATURE_PADDING);
				if (hitRateForShortVersion <= HIT_RATE_THRESHOLD) {
					// Need to increase font
					if (currentFontSize >= MAXIMUM_FONT_SIZE) {
						// No chance to increase - use max font
						currentFontSize = MAXIMUM_FONT_SIZE;
						fontSizeFound = true;
					} else {
						// Try to avoid divergency
						if (didDecreasingOnLastIteration) {
							multiplyFactor *= 2;
						}
						prevIterationFontSize = currentFontSize;
						currentFontSize /= Math.pow(hitRateForShortVersion, (1.0 / multiplyFactor));
						didDecreasingOnLastIteration = false;
					}
				} else if (hitRateForShortVersion > HIT_RATE_THRESHOLD && (hitRateForShortVersion <= 1)) {
					// Win!
					fontSizeFound = true;
				} else if (hitRateForShortVersion > 1) {
					// Need to decrease font
					if (currentFontSize <= MINIMAL_FONT_SIZE) {
						// No way to fit short version, go to tall
						useTallVersion = true;
					} else {
						// Exit loop if can not meet HIT_RATE_THRESHOLD after 20
						// iterations or so - it is already very close
						if (iterationNumber >= MAX_ITERATIONS) {
							currentFontSize = prevIterationFontSize;
							fontSizeFound = true;
							break;
						} else {
							currentFontSize /= Math.pow(hitRateForShortVersion, (1.0 / multiplyFactor));
							didDecreasingOnLastIteration = true;
						}
					}
				}
			}
			if (useTallVersion) {
				float hitRateForTallVersion = drawPosY / (PREFFERABLE_HEIGHT_TALL - SIGNATURE_FONT_SIZE - SIGNATURE_PADDING);
				if (hitRateForTallVersion <= HIT_RATE_THRESHOLD) {
					// Need to increase font
					if (currentFontSize >= MAXIMUM_FONT_SIZE) {
						// No chance to increase - use max font
						currentFontSize = MAXIMUM_FONT_SIZE;
						fontSizeFound = true;
					} else {
						// Try to avoid divergency
						if (didDecreasingOnLastIteration) {
							multiplyFactor *= 2;
						}
						prevIterationFontSize = currentFontSize;
						currentFontSize /= Math.pow(hitRateForTallVersion, (1.0 / multiplyFactor));
						didDecreasingOnLastIteration = false;
					}
				} else if (hitRateForTallVersion > HIT_RATE_THRESHOLD && (hitRateForTallVersion <= 1)) {
					// Win!
					fontSizeFound = true;
				} else if (hitRateForTallVersion > 1) {
					// Need to decrease font
					if (currentFontSize <= MINIMAL_FONT_SIZE) {
						// No chance to decrease - use min font
						currentFontSize = MINIMAL_FONT_SIZE;
						fontSizeFound = true;
					} else {
						// Exit loop if can not meet HIT_RATE_THRESHOLD after 20
						// iterations or so - it is already very close
						if (iterationNumber >= MAX_ITERATIONS) {
							currentFontSize = prevIterationFontSize;
							fontSizeFound = true;
							break;
						} else {
							currentFontSize /= Math.pow(hitRateForTallVersion, (1.0 / multiplyFactor));
							didDecreasingOnLastIteration = true;
						}
					}
				}
			}
		}
		g2d.dispose();

		return new SizeCalculationResult(currentFontSize, useTallVersion);
	}

	// Fill canvas with white
	private void clearImage(boolean useTallVersion, Graphics2D g2d) {
		g2d.setColor(BACKGROUND_COLOR);
		g2d.fillRect(0, 0, PREFFERABLE_WIDTH, useTallVersion ? PREFFERABLE_HEIGHT_TALL : PREFFERABLE_HEIGHT_SHORT);
	}

	// Make some prettiness
	private void setRenderingHints(Graphics2D g2d) {
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setColor(FONT_COLOR);
	}

	public static void renderTextIntoImageBlob(String text, String imgFormatName, BLOB imgBlob) {
		TextRenderServiceImpl instance = new TextRenderServiceImpl();
		byte[] imgBytes = instance.renderText(text, imgFormatName);
		OutputStream imgOutStr = null;
		try {
			imgOutStr = imgBlob.setBinaryStream(0);
			imgOutStr.write(imgBytes);
			imgOutStr.flush();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}
}

/

CREATE OR REPLACE PACKAGE TextRenderer AS
	FUNCTION renderTextIntoImage(text IN VARCHAR2) RETURN BLOB;
END TextRenderer;
/

CREATE OR REPLACE PACKAGE BODY TextRenderer AS	
	PROCEDURE renderTextIntoImageBlob (text IN VARCHAR2, img_format IN VARCHAR2, img_blob IN BLOB) IS
		LANGUAGE JAVA NAME 'by.knick.eli.TextRenderServiceImpl.renderTextIntoImageBlob(java.lang.String, java.lang.String, oracle.sql.BLOB)';

	FUNCTION renderTextIntoImage(text IN VARCHAR2) RETURN BLOB IS
		img_blob BLOB;
	BEGIN
		DBMS_LOB.createtemporary (img_blob, TRUE);
		renderTextIntoImageBlob(text, 'BMP', img_blob);
      	RETURN img_blob;
	END;
END TextRenderer;
/