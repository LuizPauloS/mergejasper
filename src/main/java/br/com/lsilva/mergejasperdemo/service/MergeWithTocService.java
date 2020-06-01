package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TabAlignment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MergeWithTocService {

    public static final String SRC3 = "./src/main/resources/templates/pdf/toc.pdf";
    public static final String IMG_SRC = "/templates/img/";
    public static final String PDF_SRC = "/templates/pdf/";

    public List<byte[]> getImagesFormat() throws IOException {
        return Arrays.asList(Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "cpf.jpg").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "rg.jpg").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "brasao_ms.jpg").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo.png").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_governo.png").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_imasul.jpg").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "relatorio_logo.jpg").getURI())));
    }

    public List<byte[]> getPathPDFs() throws IOException {
        return Arrays.asList(Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "hello.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "united_states.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "pdfteste.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "Comunicado.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "cv.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "extrato.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "protocolo.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "prova.pdf").getURI())),
                Files.readAllBytes(Paths.get(new ClassPathResource(PDF_SRC + "Matriz.pdf").getURI())));
    }

    public byte[] manipulatePdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        Document doc = new Document(pdfDoc);

        // Copier contains the additional logic to copy acroform fields to a new page.
        // PdfPageFormCopier uses some caching logic which can potentially improve performance
        // in case of the reusing of the same instance.
        PdfPageFormCopier formCopier = new PdfPageFormCopier();

        // Copy all merging file's pages to the temporary pdf file
        Map<Integer, PdfDocument> filesToMerge = initializeFilesToMerge();
        Map<Integer, String> toc = new TreeMap<>();
        int page = 1;
        for (Map.Entry<Integer, PdfDocument> entry : filesToMerge.entrySet()) {
            PdfDocument srcDoc = entry.getValue();
            int numberOfPages = srcDoc.getNumberOfPages();

            toc.put(page, "Teste " + entry.getKey());

            for (int i = 1; i <= numberOfPages; i++, page++) {
                Text text = new Text(String.format("%d", page));
                srcDoc.copyPagesTo(i, i, pdfDoc, formCopier);

                // Put the destination at the very first page of each merged document
                if (i == 1) {
                    text.setDestination("p" + page);

                    PdfOutline rootOutLine = pdfDoc.getOutlines(false);
                    PdfOutline outline = rootOutLine.addOutline("p" + page);
                    outline.addDestination(PdfDestination.makeDestination(new PdfString("p" + page)));
                }

                doc.add(new Paragraph(text)
                        .setFixedPosition(page, 549, 810, 40)
                        .setMargin(0)
                        .setMultipliedLeading(1));
            }
        }

        PdfDocument tocDoc = new PdfDocument(new PdfReader(SRC3));
        tocDoc.copyPagesTo(1, 1, pdfDoc, formCopier);
        tocDoc.close();

        // Create a table of contents
        float tocYCoordinate = 750;
        float tocXCoordinate = doc.getLeftMargin();
        float tocWidth = pdfDoc.getDefaultPageSize().getWidth() - doc.getLeftMargin() - doc.getRightMargin();
        for (Map.Entry<Integer, String> entry : toc.entrySet()) {
            Paragraph p = new Paragraph();
            p.addTabStops(new TabStop(500, TabAlignment.LEFT, new DashedLine()));
            p.add(entry.getValue());
            p.add(new Tab());
            p.add(String.valueOf(entry.getKey()));
            p.setAction(PdfAction.createGoTo("p" + entry.getKey()));
            doc.add(p
                    .setFixedPosition(pdfDoc.getNumberOfPages(), tocXCoordinate, tocYCoordinate, tocWidth)
                    .setMargin(0)
                    .setMultipliedLeading(1));

            tocYCoordinate -= 20;
        }

        for (PdfDocument srcDoc : filesToMerge.values()) {
            srcDoc.close();
        }

        doc.close();

        PdfDocument resultDoc = new PdfDocument(new PdfWriter(baos));
        PdfDocument srcDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(baos.toByteArray()),
                new ReaderProperties()));
        srcDoc.initializeOutlines();

        // Create a copy order list and set the page with a table of contents as the first page
        int tocPageNumber = srcDoc.getNumberOfPages();
        List<Integer> copyPagesOrderList = new ArrayList<>();
        copyPagesOrderList.add(tocPageNumber);
        for(int i = 1; i < tocPageNumber; i++) {
            copyPagesOrderList.add(i);
        }

        srcDoc.copyPagesTo(copyPagesOrderList, resultDoc, formCopier);

        srcDoc.close();
        resultDoc.close();
        return baos.toByteArray();
    }

    private Map<Integer, PdfDocument> initializeFilesToMerge() throws Exception {
        List<byte[]> pdfs = createListPdfAndImages();
        Map<Integer, PdfDocument> filesToMerge = new TreeMap<>();
        for (int i = 0; i < pdfs.size(); i++) {
            filesToMerge.put(i, new PdfDocument(new PdfReader(
                    new ByteArrayInputStream(pdfs.get(i)), new ReaderProperties())));
        }
        return filesToMerge;
    }

    private List<byte[]> createListPdfAndImages() throws Exception {
        List<byte[]> list = getImagesFormat().stream().map(this::generatePdfDocumentImage).collect(Collectors.toList());
        list.addAll(generatedCopyPDFExisting());
        return list;
    }

    private List<byte[]> generatedCopyPDFExisting() throws Exception {
        return new ArrayList<>(getPathPDFs());
    }

    public byte[] generatePdfDocumentImage(byte[] image) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(result);
        PdfDocument copy = new PdfDocument(new PdfWriter(pdfWriter));
        Document document = new Document(copy);

        float leftMargin = document.getLeftMargin(), rightMargin = document.getRightMargin();
        float topMargin = document.getTopMargin(), bottomMargin = document.getBottomMargin();
        float pdfA4usableWidth = PageSize.A4.getWidth() - leftMargin - rightMargin;
        float pdfA4usableHeight = PageSize.A4.getHeight() - topMargin - bottomMargin;

        ImageData imageData = ImageDataFactory.create(image);
        Image img = new Image(imageData);
        img.scaleToFit(pdfA4usableWidth, pdfA4usableHeight);
        float x = (PageSize.A4.getWidth() - img.getImageScaledWidth()) / 2;
        float y = (PageSize.A4.getHeight() - img.getImageScaledHeight()) / 2;
        img.setFixedPosition(1, x, y);

        document.add(img);
        document.close();
        copy.close();

        return result.toByteArray();
    }

}
