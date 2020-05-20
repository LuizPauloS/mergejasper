package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TesteService {

    public static final String IMG_SRC = "/templates/img/";
    public static final String PDF_SRC = "/templates/pdf/pdfteste.pdf";

    public byte[] getPdfFormat() throws IOException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(PDF_SRC), new PdfWriter(byteOutputStream));
        Document doc = new Document(pdfDoc);

        int numberOfPages = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            // Write aligned text to the specified by parameters point
            doc.showTextAligned(new Paragraph(String.format("page %s of %s", i, numberOfPages)),
                    559, 806, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0);
        }
        doc.close();
        return byteOutputStream.toByteArray();
    }

    public byte[] getImagesFormat() throws IOException {
        byte[] img = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "cpf.jpg").getURI()));
        byte[] img2 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "rg.jpg").getURI()));

        List<byte[]> documents = Arrays.asList(img, img2);

        for (byte[] image: documents) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(byteArrayOutputStream));
            final Document imageDocument = new Document(pdfDoc);

            Image image = ;
//                image.scaleToFit(550, 500);
//                image.setAbsolutePosition(10, 300);
//                imageDocument.add(image);
//
//                imageDocument.close();
//                pdfReader = new PdfReader(byteStream.toByteArray());
        }

        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfReader(PDF_SRC), new PdfWriter(byteOutputStream));
        Document doc = new Document(pdfDoc);

        int numberOfPages = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            // Write aligned text to the specified by parameters point
            doc.showTextAligned(new Paragraph(String.format("page %s of %s", i, numberOfPages)),
                    559, 806, i, TextAlignment.RIGHT, VerticalAlignment.TOP, 0);
        }
        doc.close();
        return byteOutputStream.toByteArray();
    }

//    ByteArrayOutputStream result = null;
//		try {
//        result = new ByteArrayOutputStream();
//        PdfCopyFields copy = new PdfCopyFields(result);
//        for (byte[] document: documents) {
//
//            InputStream is = new BufferedInputStream(new ByteArrayInputStream(document));
//            String mimeType = URLConnection.guessContentTypeFromStream(is);
//            PdfReader pdfReader = null;
//
//            if(mimeType == null) {
//
//                pdfReader = new PdfReader(document);
//
//            } else if(mimeType.endsWith("jpg") || mimeType.endsWith("png")) {
//                final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//
//                final Document imageDocument = new Document(PageSize.A4);
//                PdfWriter pdfWriter = PdfWriter.getInstance(imageDocument, byteStream);
//                imageDocument.open();
//
//                // Create single page with the dimensions as source image and no margins:
//                Image image = Image.getInstance(document);
//                image.scaleToFit(550, 500);
//                image.setAbsolutePosition(10, 300);
//                imageDocument.add(image);
//
//                imageDocument.close();
//                pdfReader = new PdfReader(byteStream.toByteArray());
//            }
//            copy.addDocument(pdfReader);
//        }
}
