package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TabAlignment;
import org.bouncycastle.jcajce.provider.symmetric.DES;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service
public class MergeWithToc4Service {

    public static final String DEST = "./src/main/resources/templates/temp/merge_with_toc2.pdf";
    public static final String SRC = "./src/main/resources/templates/pdf/eiarima.pdf";
    public static final String TOC = "./src/main/resources/templates/pdf/toc.pdf";
    public static final String SRC2 = "./src/main/resources/templates/pdf/java8.pdf";
    public static final String SRC3 = "./src/main/resources/templates/pdf/teste.pdf";

    public void criaDiretorio() {
        File file = new File(DEST);
        file.getParentFile().mkdirs();
    }

    public File manipulatePdf() throws IOException {
        PdfWriter writer = new PdfWriter(DEST, new WriterProperties().setFullCompressionMode(true));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc);

        pdfDoc.initializeOutlines();

        PdfPageFormCopier formCopier = new PdfPageFormCopier();

        Map<String, PdfDocument> filesToMerge = initializeFilesToMerge();
        Map<Integer, String> toc = geraIndice(pdfDoc, formCopier, filesToMerge);

        PdfDocument tocDoc = new PdfDocument(new PdfReader(TOC));
        tocDoc.copyPagesTo(1, 1, pdfDoc, formCopier);
        tocDoc.getReader().close();
        tocDoc.close();

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
            srcDoc.getReader().close();
            srcDoc.close();
        }

        doc.close();
        return new File(DEST);
    }

    private Map<Integer, String> geraIndice(PdfDocument pdfDoc, PdfPageFormCopier formCopier,
                                            Map<String, PdfDocument> filesToMerge) {
        Map<Integer, String> toc = new TreeMap<>();
        int page = 1;
        for (Map.Entry<String, PdfDocument> entry : filesToMerge.entrySet()) {
            PdfDocument srcDoc = entry.getValue();
            int numberOfPages = srcDoc.getNumberOfPages();

            toc.put(page, entry.getKey());
            float percentage = 0.9f;
            for (int i = 1; i <= numberOfPages; i++, page++) {
                Text text = new Text(String.format("%d", page));
                srcDoc.getReader().setUnethicalReading(true);
                srcDoc.copyPagesTo(i, i, pdfDoc, formCopier);

                if (i == 1) {
                    text.setDestination("p" + page);
                }

                PdfPage pdfPage = pdfDoc.getLastPage();
                Rectangle pageSize = pdfPage.getCropBox();

                float offsetX = (pageSize.getWidth() * (1 - percentage)) / 2;
                float offsetY = (pageSize.getHeight() * (1 - percentage)) / 2;

                new PdfCanvas(pdfPage.newContentStreamBefore(), pdfPage.getResources(), pdfDoc)
                        .writeLiteral(String.format(Locale.getDefault(), "\nq %s 0 0 %s %s %s cm\nq\n",
                                percentage, percentage, offsetX, offsetY));

                new PdfCanvas(pdfPage.newContentStreamAfter(), pdfPage.getResources(), pdfDoc)
                        .writeLiteral("\nQ\nQ\n");
                try (Canvas canvas = new Canvas(new PdfCanvas(pdfPage, true), pageSize)) {
                    canvas.add(new Paragraph(text)
                            .setFixedPosition(page, pageSize.getWidth() - 55, pageSize.getHeight() - 30, 40));
                }
            }
        }
        return toc;
    }

    private static Map<String, PdfDocument> initializeFilesToMerge() throws IOException {
        Map<String, PdfDocument> filesToMerge = new TreeMap<>();
        for (int i = 0; i < 10; i++) {
            filesToMerge.put(i + " - EIARIMA", new PdfDocument(new PdfReader(SRC)));
            filesToMerge.put(i + " - JAVA 8 - LAMBDA", new PdfDocument(new PdfReader(SRC2)));
            filesToMerge.put(i + " - TESTE DOCUMENTO ORIGINAL", new PdfDocument(new PdfReader(SRC3)));
            filesToMerge.put(i + " - TESTE TOC", new PdfDocument(new PdfReader(TOC)));
        }
//        filesToMerge.put("01 - EIARIMA", new PdfDocument(new PdfReader(SRC)));
//        filesToMerge.put("02 - JAVA 8 - LAMBDA", new PdfDocument(new PdfReader(SRC2)));
//        filesToMerge.put("03 - TESTE DOCUMENTO ORIGINAL", new PdfDocument(new PdfReader(SRC3)));
        return filesToMerge;
    }
}
