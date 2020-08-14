package br.com.lsilva.mergejasperdemo.service;

import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Tab;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.property.TabAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class MergeWithToc2Service {
    public static final String DEST = "./target/sandbox/merge/merge_with_toc2.pdf";

    public static final String SRC1 = "./src/main/resources/templates/pdf/java8.pdf";
    public static final String SRC2 = "./src/main/resources/templates/pdf/eiarima.pdf";
    public static final String SRC3 = "./src/main/resources/templates/pdf/toc.pdf";
    public static final String SRC_CAPA = "./src/main/resources/templates/pdf/capa-processo.pdf";

    public void criaDiretorio() {
        File file = new File(DEST);
        file.getParentFile().mkdirs();
    }

    public File manipulatePdf(String dest) throws IOException {
        criaDiretorio();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
        final Document doc = new Document(pdfDoc);

        // Copier contains the additional logic to copy acroform fields to a new page.
        // PdfPageFormCopier uses some caching logic which can potentially improve performance
        // in case of the reusing of the same instance.
        final PdfPageFormCopier formCopier = new PdfPageFormCopier();

        // Copy all merging file's pages to the temporary pdf file
        Map<String, PdfDocument> filesToMerge = initializeFilesToMerge();
        Map<Integer, String> toc = new TreeMap<Integer, String>();
        int page = 1;
        for (Map.Entry<String, PdfDocument> entry : filesToMerge.entrySet()) {
            final PdfDocument srcDoc = entry.getValue();
            int numberOfPages = srcDoc.getNumberOfPages();

            toc.put(page, entry.getKey());

            for (int i = 1; i <= numberOfPages; i++, page++) {
                Text text = new Text(String.format("Page %d", page));
                srcDoc.getReader().setUnethicalReading(true);
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

        final PdfDocument tocDoc = new PdfDocument(new PdfReader(SRC3));
        tocDoc.copyPagesTo(1, 1, pdfDoc, formCopier);
        tocDoc.getReader().close();
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
            srcDoc.getReader().close();
            srcDoc.close();
        }

        doc.close();

        final PdfDocument resultDoc = new PdfDocument(new PdfWriter(dest));
        final PdfDocument srcDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(baos.toByteArray()),
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
        return new File(dest);
    }

    private static Map<String, PdfDocument> initializeFilesToMerge() throws IOException {
        Map<String, PdfDocument> filesToMerge = new TreeMap<String, PdfDocument>();
        filesToMerge.put("01 Countries", new PdfDocument(new PdfReader(SRC1)));
        filesToMerge.put("02 Hello World", new PdfDocument(new PdfReader(SRC2)));
        return filesToMerge;
    }
}
