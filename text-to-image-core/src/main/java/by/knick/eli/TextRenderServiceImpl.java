package by.knick.eli;

import java.awt.Color;
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

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleDriver;
import oracle.sql.BLOB;

public class TextRenderServiceImpl implements TextRenderService {

	private static final int PREFFERABLE_WIDTH = 198;
	private static final int PREFFERABLE_HEIGHT_SHORT = 68;
	private static final int PREFFERABLE_HEIGHT_TALL = 198;

	private static final int MINIMAL_FONT_SIZE = 10;
	private static final int MAXIMUM_FONT_SIZE = PREFFERABLE_HEIGHT_SHORT;

	private static final String FONT_FAMILY = "Serif";

	private static final double THRESHOLD = 0.97;
	private static final int MAX_ITERATIONS = 10;

	@Override
	public byte[] renderText(String text) {
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

		BufferedImage img = new BufferedImage(PREFFERABLE_WIDTH, useTallVersion ? PREFFERABLE_HEIGHT_TALL : PREFFERABLE_HEIGHT_SHORT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();
		clearImage(useTallVersion, g2d);
		setRenderingHints(g2d);
		FontRenderContext frc = g2d.getFontRenderContext();

		LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);

		// Set break width to width of Component.
		float breakWidth = PREFFERABLE_WIDTH;
		float drawPosY = 0;
		// Set position to the index of the first character in the paragraph.
		lineMeasurer.setPosition(paragraphStart);

		// Get lines until the entire paragraph has been displayed.
		while (lineMeasurer.getPosition() < paragraphEnd) {

			// Retrieve next layout. A cleverer program would also cache
			// these layouts until the component is re-sized.
			TextLayout layout = lineMeasurer.nextLayout(breakWidth);

			// Compute pen x position. If the paragraph is right-to-left we
			// will align the TextLayouts to the right edge of the panel.
			// Note: this won't occur for the English text in this sample.
			// Note: drawPosX is always where the LEFT of the text is placed.
			float drawPosX = layout.isLeftToRight() ? 0 : breakWidth - layout.getAdvance();

			// Move y-coordinate by the ascent of the layout.
			drawPosY += layout.getAscent();

			// Draw the TextLayout at (drawPosX, drawPosY).
			layout.draw(g2d, drawPosX, drawPosY);

			// Move y-coordinate in preparation for next layout.
			drawPosY += layout.getDescent() + layout.getLeading();
		}

		g2d.dispose();

		// Write to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(img, "BMP", baos);
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

			float drawPosY = 0;
			// Get lines until the entire paragraph has been displayed.
			while (lineMeasurer.getPosition() < paragraph.getEndIndex()) {
				// Retrieve next row layout
				TextLayout layout = lineMeasurer.nextLayout(PREFFERABLE_WIDTH);
				// Move y-coordinate by the font size
				drawPosY += layout.getAscent() + layout.getDescent() + layout.getLeading();
			}

			if (!useTallVersion) {
				float hitRateForShortVersion = drawPosY / PREFFERABLE_HEIGHT_SHORT;
				if (hitRateForShortVersion <= THRESHOLD) {
					// Need to increase font
					if (currentFontSize >= MAXIMUM_FONT_SIZE) {
						// No chance to increase - use max font
						currentFontSize = MAXIMUM_FONT_SIZE;
						fontSizeFound = true;
					} else {
						if (didDecreasingOnLastIteration) {
							multiplyFactor++;
						}
						if (iterationNumber >= MAX_ITERATIONS) {
							fontSizeFound = true;
							break;
						}
						currentFontSize /= Math.pow(hitRateForShortVersion, (1.0 / multiplyFactor));
						didDecreasingOnLastIteration = false;
					}
				} else if (hitRateForShortVersion > THRESHOLD && hitRateForShortVersion <= 1) {
					// Win!
					fontSizeFound = true;
				} else if (hitRateForShortVersion > 1) {
					// Need to decrease font
					if (currentFontSize <= MINIMAL_FONT_SIZE) {
						// No way to fit short version, go to tall
						useTallVersion = true;
					} else {
						currentFontSize /= Math.pow(hitRateForShortVersion, (1.0 / multiplyFactor));
						didDecreasingOnLastIteration = true;
					}
				}
			}
			if (useTallVersion) {
				float hitRateForTallVersion = drawPosY / PREFFERABLE_HEIGHT_TALL;
				if (hitRateForTallVersion <= THRESHOLD) {
					// Need to increase font
					if (currentFontSize >= MAXIMUM_FONT_SIZE) {
						// No chance to increase - use max font
						currentFontSize = MAXIMUM_FONT_SIZE;
						fontSizeFound = true;
					} else {
						if (iterationNumber >= MAX_ITERATIONS) {
							fontSizeFound = true;
							break;
						}
						if (didDecreasingOnLastIteration) {
							multiplyFactor++;
						}
						currentFontSize /= Math.pow(hitRateForTallVersion, (1.0 / multiplyFactor));
						didDecreasingOnLastIteration = false;
					}
				} else if (hitRateForTallVersion > THRESHOLD && hitRateForTallVersion <= 1) {
					// Win!
					fontSizeFound = true;
				} else if (hitRateForTallVersion > 1) {
					// Need to decrease font
					if (currentFontSize <= MINIMAL_FONT_SIZE) {
						// No chance to decrease - use min font
						currentFontSize = MINIMAL_FONT_SIZE;
						fontSizeFound = true;
					} else {
						currentFontSize /= Math.pow(hitRateForTallVersion, (1.0 / multiplyFactor));
						didDecreasingOnLastIteration = true;
					}
				}
			}
		}
		g2d.dispose();

		return new SizeCalculationResult(currentFontSize, useTallVersion);
	}

	private void clearImage(boolean useTallVersion, Graphics2D g2d) {
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, PREFFERABLE_WIDTH, useTallVersion ? PREFFERABLE_HEIGHT_TALL : PREFFERABLE_HEIGHT_SHORT);
	}

	private void setRenderingHints(Graphics2D g2d) {
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setColor(Color.BLACK);
	}

	public static BLOB renderTextIntoImage(String text) {
		TextRenderService instance = new TextRenderServiceImpl();
		byte[] imgBytes = instance.renderText(text);

		BLOB imgBlob = null;
		OutputStream imgOutStr = null;
		try {
			OracleConnection conn = (OracleConnection) new OracleDriver().defaultConnection();
			imgBlob = BLOB.createTemporary(conn, true, oracle.sql.BLOB.DURATION_SESSION);
			imgOutStr = imgBlob.setBinaryStream(0);
			imgOutStr.write(imgBytes);
			imgOutStr.flush();
		} catch (Throwable e){
			throw new RuntimeException(e);
		}		
		return imgBlob;
	}
}
