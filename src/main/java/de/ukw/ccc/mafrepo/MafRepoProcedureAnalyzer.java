/*
 * MIT License
 *
 * Copyright (c) 2023 Comprehensive Cancer Center Mainfranken
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.ukw.ccc.mafrepo;

import de.itc.onkostar.api.Disease;
import de.itc.onkostar.api.IOnkostarApi;
import de.itc.onkostar.api.Procedure;
import de.itc.onkostar.api.analysis.AnalyzerRequirement;
import de.itc.onkostar.api.analysis.IProcedureAnalyzer;
import de.itc.onkostar.api.analysis.OnkostarPluginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MafRepoProcedureAnalyzer implements IProcedureAnalyzer {

    /**
     * Logger for this class.
     * Provides better log output than {@code System.out.println()}'
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private IOnkostarApi onkostarApi;

    private JdbcTemplate jdbcTemplate;

    private RestTemplate restTemplate;

    public MafRepoProcedureAnalyzer(IOnkostarApi onkostarApi, DataSource dataSource) {
        this.onkostarApi = onkostarApi;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.restTemplate = new RestTemplate();
    }

    @Override
    public OnkostarPluginType getType() {
        return OnkostarPluginType.BACKEND_SERVICE;
    }

    @Override
    public String getVersion() {
        return "0.1.0";
    }

    @Override
    public String getName() {
        return "Backend Service für das MAF-Repo";
    }

    @Override
    public String getDescription() {
        return "Backend Service für das MAF-Repo";
    }

    @Override
    public boolean isRelevantForDeletedProcedure() {
        return false;
    }

    @Override
    public boolean isRelevantForAnalyzer(Procedure procedure, Disease disease) {
        return false;
    }

    @Override
    public void analyze(Procedure procedure, Disease disease) {
        // Nothing to do
    }

    @Override
    public boolean isSynchronous() {
        return false;
    }

    @Override
    public AnalyzerRequirement getRequirement() {
        return AnalyzerRequirement.PROCEDURE;
    }

    public List<?> requestSimpleVariants(Map<String, Object> input) {
        var sampleId = input.get("sampleId");
        if (sampleId == null) {
            throw new RuntimeException("No SampleID given!");
        }

        var mafrepoUrl = this.onkostarApi.getGlobalSetting("mafrepo_url");

        if (null == mafrepoUrl) {
            throw new RuntimeException("Einstellung 'marfrepo_url' nicht vorhanden");
        }

        var uri = UriComponentsBuilder.fromUriString(mafrepoUrl)
                .path("/samples/{sampleId}/simplevariants")
                .buildAndExpand(sampleId)
                .toUri();

        ResponseEntity<SimpleVariantResponse[]> responseEntity = this.restTemplate.getForEntity(uri, SimpleVariantResponse[].class);

        SimpleVariantResponse[] response = responseEntity.getBody();

        return Arrays.stream(response).map((item) -> {
            var mappedItem = new HashMap<String, Object>();
            mappedItem.put("Dokumentation", Map.of("val", "ERW", "version", findVersion("OS.MolDokumentation")));
            mappedItem.put("Ergebnis", Map.of("val", "P", "version", findVersion("OS.MolGenErgebnis")));
            mappedItem.put("EVChromosom", Map.of("val", orEmpty(item.chromosome), "version", findVersion("OS.MolDiagFusionChromosome")));
            mappedItem.put("Untersucht", Map.of("val", orEmpty(item.hugoSymbol), "version", findVersion("OS.Molekulargenetik")));
            mappedItem.put("cDNANomenklatur", orEmpty(item.hgvsc));
            mappedItem.put("ProteinebeneNomenklatur", orEmpty(item.hgvsp));
            mappedItem.put("ExonInt", orEmpty(item.exon));

            mappedItem.put("EVENSEMBLID", orEmpty(item.gene));
            mappedItem.put("EVHGNCID", orEmpty(item.hgncId));
            mappedItem.put("EVHGNCSymbol", orEmpty(item.hugoSymbol));
            mappedItem.put("EVHGNCName", orEmpty(item.geneName));
            mappedItem.put("EVStart", orEmpty(item.startPosition.toString()));
            mappedItem.put("EVEnde", orEmpty(item.endPosition.toString()));
            mappedItem.put("EVAltNucleotide", orEmpty(item.tumorSeqAllele2));
            mappedItem.put("EVRefNucleotide", orEmpty(item.referenceAllele));
            mappedItem.put("EVNMNummer", orEmpty(item.nmNumber));
            mappedItem.put("Coverage", orEmpty(item.tdepth.toString()));
            mappedItem.put("Allelfrequenz", orEmpty(String.format(Locale.GERMAN,"%.3f", item.allelicFrequency * 100.)));
            mappedItem.put("EVdbSNPID", orEmpty(item.dbSnpRs));

            mappedItem.put("ExonText", orEmpty(item.exon));
            return mappedItem;
        }).collect(Collectors.toList());
    }

    private Long findVersion(String name) {
        var sql = "SELECT pcv.id FROM property_catalogue_version pcv "
                + "JOIN property_catalogue pc ON pc.id = pcv.datacatalog_id "
                + "WHERE name = ?";

        try {
            var r = jdbcTemplate.queryForObject(sql, new Object[]{name}, Long.class);
            if (null != r) {
                return r;
            }
        } catch (Exception e) {
            logger.error("Nothing found: ", e);
        }
        logger.error("Error on '{}'", name);
        return 0L;
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }

    static class SimpleVariantResponse {
        public String tumorSampleBarcode;
        public String hugoSymbol;
        public String chromosome;
        public String gene;
        public Long startPosition;
        public Long endPosition;
        public String referenceAllele;
        public String tumorSeqAllele2;
        public String hgvsc;
        public String hgvsp;
        public String exon;
        public Long tdepth;
        public String dbSnpRs;
        public String panel;
        public Double allelicFrequency;
        public String cosmicId;
        public String interpretation;
        public String hgncId;
        public String geneName;
        public String nmNumber;
    }
}


