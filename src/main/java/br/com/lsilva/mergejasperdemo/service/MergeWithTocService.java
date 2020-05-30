package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.ReaderProperties;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.TabAlignment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MergeWithTocService {

    public static final String SRC3 = "./src/main/resources/templates/pdf/toc.pdf";
    public static final String IMG_SRC = "/templates/img/";
    public static final String PDF_SRC = "/templates/pdf/";

    public List<byte[]> getImagesFormat() throws IOException {
        byte[] img = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "cpf.jpg").getURI()));
        byte[] img2 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "rg.jpg").getURI()));
        byte[] img3 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "brasao_ms.jpg").getURI()));
        byte[] img4 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo.png").getURI()));
        byte[] img5 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_governo.png").getURI()));
        byte[] img6 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "logo_imasul.jpg").getURI()));
        byte[] img7 = Files.readAllBytes(Paths.get(new ClassPathResource(IMG_SRC + "relatorio_logo.jpg").getURI()));
        return Arrays.asList(img, img2, img3, img4, img5, img6, img7);
    }

    public List<String> getPathPDFs() {
        return Arrays.asList(new ClassPathResource(PDF_SRC + "hello.pdf").getPath(),
                new ClassPathResource(PDF_SRC + "united_states.pdf").getPath());
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
        Map<String, PdfDocument> filesToMerge = initializeFilesToMerge();
        Map<Integer, String> toc = new TreeMap<>();
        int page = 1;
        for (Map.Entry<String, PdfDocument> entry : filesToMerge.entrySet()) {
            PdfDocument srcDoc = entry.getValue();
            int numberOfPages = srcDoc.getNumberOfPages();

            toc.put(page, entry.getKey());

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

    private Map<String, PdfDocument> initializeFilesToMerge() throws Exception {
        List<byte[]> pdfs = createListPdfImages();
        //pdfs.addAll(generatedCopyPDFExisting());
        Map<String, PdfDocument> filesToMerge = new TreeMap<>();
        int count = 0;
        for (byte[] pdf: pdfs) {
            filesToMerge.put("Teste " + count++, new PdfDocument(new PdfReader(
                    new ByteArrayInputStream(pdf), new ReaderProperties())));
        }
        return filesToMerge;
    }

    public List<byte[]> createListPdfImages() throws Exception {
        return getImagesFormat().stream().map(this::generatePdfDocumentImage).collect(Collectors.toList());
    }

    public List<byte[]> generatedCopyPDFExisting() {
        return getPathPDFs().stream().map(path -> {
            try {
                return generatedCopyPDFExisting(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new byte[0];
        }).collect(Collectors.toList());
    }

    public byte[] generatedCopyPDFExisting(String path) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        //TODO add PDFs on here
        return result.toByteArray();
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
