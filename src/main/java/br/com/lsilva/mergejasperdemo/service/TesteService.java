package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

@Service
public class TesteService {

    public static final String IMG_SRC = "/templates/img/";

    public List<byte[]> getImagesFormat() throws IOException {
        byte[] img = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "cpf.jpg").getURI()));
        byte[] img2 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "rg.jpg").getURI()));
        byte[] img3 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "brasao_ms.jpg").getURI()));
        byte[] img4 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo.png").getURI()));
        byte[] img5 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_governo.png").getURI()));
        byte[] img6 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_imasul.jpg").getURI()));
        byte[] img7 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "relatorio_logo.jpg").getURI()));
        byte[] img8 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "teste.jpg").getURI()));

        return Arrays.asList(img, img2, img3, img4, img5, img6, img7, img8);
    }

    public byte[] getPdfDocumentImage() throws IOException {
        List<byte[]> images = getImagesFormat();
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(result);
        PdfDocument copy = new PdfDocument(new PdfWriter(pdfWriter));
        Document document = new Document(copy);

        float leftMargin = document.getLeftMargin(), rightMargin = document.getRightMargin();
        float topMargin = document.getTopMargin(), bottomMargin = document.getBottomMargin();
        float pdfA4usableWidth = PageSize.A4.getWidth() - leftMargin - rightMargin;
        float pdfA4usableHeight = PageSize.A4.getHeight() - topMargin - bottomMargin;

        for (int i = 0; i < images.size(); i++) {
            ImageData imageData = ImageDataFactory.create(images.get(i));
            Image img = new Image(imageData);
            img.scaleToFit(pdfA4usableWidth, pdfA4usableHeight);
            float x = (PageSize.A4.getWidth() - img.getImageScaledWidth()) / 2;
            float y = (PageSize.A4.getHeight() - img.getImageScaledHeight()) / 2;
            img.setFixedPosition(i+1, x, y);
            document.add(img);
        }

        int numberOfPages = copy.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            // Write aligned text to the specified by parameters point
            document.showTextAligned(new Paragraph(String.format("page %s of %s", i, numberOfPages)),
                    559, 806, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0);
        }

        document.close();
        return result.toByteArray();
    }
}
