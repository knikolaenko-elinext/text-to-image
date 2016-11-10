package com.knick.eli.texttoimage.web;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import by.knick.eli.TextRenderService;
import by.knick.eli.TextRenderServiceImpl;

@Controller
@RequestMapping(value = "/convert")
public class TextToImageController {

	private TextRenderService renderService = new TextRenderServiceImpl();

	@RequestMapping(method = RequestMethod.GET)
	public void convert(@RequestParam(defaultValue = "Please specify string in 'text' request parameter") String text, HttpServletResponse response) throws IOException {
		byte[] imageBytes = renderService.renderText(text);

		response.setContentType("image/bmp");
		try (ServletOutputStream outputStream = response.getOutputStream()) {
			IOUtils.write(imageBytes, outputStream);
		}
	}
}
