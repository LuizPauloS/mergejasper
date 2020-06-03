package br.com.lsilva.mergejasperdemo.service;

import br.com.lsilva.mergejasperdemo.model.Documento;
import br.com.lsilva.mergejasperdemo.model.ProcessoDigital;
import br.com.lsilva.mergejasperdemo.repository.ProcessoRepository;
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
import com.itextpdf.layout.property.AreaBreakType;
import com.itextpdf.layout.property.TabAlignment;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.print.Doc;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MergeWithTocService {

    @Autowired
    private ProcessoRepository repository;

    public byte[] manipulatePdf(Integer id) throws Exception {
        Optional<ProcessoDigital> optionalProcesso = this.repository.findById(id);
        if (!optionalProcesso.isPresent()) {
           throw new RuntimeException("Processo informado não encontrado na base de dados!");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(baos, new WriterProperties().setPdfVersion(PdfVersion.PDF_2_0));
        PdfDocument pdfDoc = new PdfDocument(pdfWriter);
        Document doc = new Document(pdfDoc);

        // Copier contains the additional logic to copy acroform fields to a new page.
        // PdfPageFormCopier uses some caching logic which can potentially improve performance
        // in case of the reusing of the same instance.
        PdfPageFormCopier formCopier = new PdfPageFormCopier();

        // Copy all merging file's pages to the temporary pdf file
        Map<String, Map<String, PdfDocument>> filesToMerge = initializeFilesToMerge(optionalProcesso.get());
        Map<Integer, String> toc = new TreeMap<>();
        int page = 1;
        for (Map.Entry<String, Map<String, PdfDocument>> entry : filesToMerge.entrySet()) {
            //PdfDocument srcDoc = entry.getValue();
            Map<String, PdfDocument> mapDocument = entry.getValue();
            for (Map.Entry<String, PdfDocument> entryDocument : mapDocument.entrySet()) {
                PdfDocument srcDoc = entryDocument.getValue();
                int numberOfPages = srcDoc.getNumberOfPages();

                toc.put(page, entryDocument.getKey());

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
            for (PdfDocument srcDoc : mapDocument.values()) {
                srcDoc.close();
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream, new WriterProperties().setPdfVersion(PdfVersion.PDF_2_0));
        writer.setCloseStream(false);
        PdfDocument tocDoc = new PdfDocument(writer);
        tocDoc.addNewPage();
        tocDoc.close();

        tocDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(outputStream.toByteArray())));
        tocDoc.copyPagesTo(1, tocDoc.getNumberOfPages(), pdfDoc, formCopier);
        tocDoc.close();
        outputStream.close();

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
            doc.add(p.setFixedPosition(pdfDoc.getNumberOfPages(), tocXCoordinate, tocYCoordinate, tocWidth)
                    .setMargin(0).setMultipliedLeading(1));
            tocYCoordinate -= 20;
        }

        doc.close();

        PdfDocument resultDoc = new PdfDocument(pdfWriter);
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

    private Map<String, Map<String, PdfDocument>> initializeFilesToMerge(ProcessoDigital processo) throws Exception {
        List<Documento> documentosPrioridade = processo.getDocumentos().stream()
                .filter(this::isRepeitarPrioridade).collect(Collectors.toList());
        List<Documento> documentosDataCriacao = processo.getDocumentos().stream()
                .filter(this::isNaoRepeitarPrioridade).sorted(Comparator.comparing(Documento::getDataCriacao))
                .collect(Collectors.toList());
        TreeMap<String, Map<String, PdfDocument>> filesToMerge = new TreeMap<>();
        //Adiciona na lista documentos com prioridade
        addListDocumentsByPriority(documentosPrioridade, filesToMerge);
        //Adiciona na lista documentos sem prioridade mas ordenado pela data criação
        addListDocumentsByCreationDate(documentosDataCriacao, filesToMerge);
        return filesToMerge;
    }

    private void addListDocumentsByPriority(List<Documento> documents,
                                            TreeMap<String, Map<String, PdfDocument>> filesToMerge) throws IOException {
        for (int i = 0; i < documents.size(); i++) {
            Map<String, PdfDocument> map = new HashMap<>();
            byte[] doc = generatePdfDocumentImage(documents.get(i));
            map.put(documents.get(i).getNome(), new PdfDocument(new PdfReader(new ByteArrayInputStream(doc), new ReaderProperties())));
            filesToMerge.put(filesToMerge.containsKey(documents.get(i).getPrioridade().toString()) ?
                    documents.get(i).getPrioridade().toString().concat(".") + i : documents.get(i).getPrioridade().toString(), map);
        }
    }

    private void addListDocumentsByCreationDate(List<Documento> documents,
                                                TreeMap<String, Map<String, PdfDocument>> filesToMerge) throws IOException {
        Integer lastKey = Integer.valueOf(FilenameUtils.getBaseName(filesToMerge.lastKey()));
        if (!documents.isEmpty() && lastKey != null) {
            for (Documento documento: documents) {
                lastKey++;
                Map<String, PdfDocument> map = new HashMap<>();
                byte[] doc = generatePdfDocumentImage(documento);
                map.put(documento.getNome(), new PdfDocument(new PdfReader(new ByteArrayInputStream(doc), new ReaderProperties())));
                filesToMerge.put(lastKey.toString(), map);
            }
        }
    }

    private boolean isRepeitarPrioridade(Documento documento) {
        return !isNaoRepeitarPrioridade(documento);
    }

    private boolean isNaoRepeitarPrioridade(Documento documento) {
        return !documento.isRespeitarPrioridade();
    }

    private byte[] generateByteDocumentPDF(String arquivo) throws IOException {
        return Files.readAllBytes(new File(arquivo).toPath());
    }

    public byte[] generatePdfDocumentImage(Documento documento) throws IOException {
        byte[] byteDocument = generateByteDocumentPDF(documento.getArquivo());

        String extensao = FilenameUtils.getExtension(documento.getArquivo());
        if (extensao != null && !(extensao.endsWith("png") || extensao.endsWith("jpg"))) {
            return byteDocument;
        }

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        PdfWriter pdfWriter = new PdfWriter(result, new WriterProperties().setPdfVersion(PdfVersion.PDF_2_0));
        PdfDocument copy = new PdfDocument(new PdfWriter(pdfWriter));
        Document document = new Document(copy);

        float leftMargin = document.getLeftMargin(), rightMargin = document.getRightMargin();
        float topMargin = document.getTopMargin(), bottomMargin = document.getBottomMargin();
        float pdfA4usableWidth = PageSize.A4.getWidth() - leftMargin - rightMargin;
        float pdfA4usableHeight = PageSize.A4.getHeight() - topMargin - bottomMargin;

        ImageData imageData = ImageDataFactory.create(byteDocument);
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
