package com.fidelg.documentgenerator.domain.type;

import com.fidelg.documentgenerator.domain.demand.CollectionItem;
import com.fidelg.documentgenerator.domain.demand.FactNode;
import com.fidelg.documentgenerator.domain.demand.FactNodeKind;
import com.fidelg.documentgenerator.domain.demand.Section;
import com.fidelg.documentgenerator.domain.demand.SectionKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tipo de documento "Demanda de Alimentos". Toda la especificidad de este
 * formato (estructura de secciones, plantillas de hechos y textos de
 * generación) vive aquí: el resto del sistema trabaja contra {@link DocumentType}.
 *
 * <p>Los textos de generación reproducen la estructura funcional aprobada. El
 * contenido jurídico final siempre lo aporta o edita el usuario en el textarea:
 * aquí solo se produce el contenido inicial.</p>
 */
public final class AlimentosDemandType implements DocumentType {

    public static final String ID = "ALIMENTOS";

    private static final String FACT_LABEL_INPUT = "rotulo";
    private static final List<SectionSpec> SECTIONS = buildSections();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String displayName() {
        return "Demanda de Alimentos";
    }

    @Override
    public List<SectionSpec> sections() {
        return SECTIONS;
    }

    @Override
    public String generateContent(SectionSpec spec, Section section) {
        return switch (spec.id()) {
            case "titulo" -> singleLine(section.input("titulo"));
            case "sumilla" -> singleLine(section.input("sumilla"));
            case "encabezamiento" -> generateEncabezamiento(section);
            case "demandado" -> generateDemandado(section);
            case "petitorio" -> generatePetitorio(section);
            case "juridicos" -> generateJuridicos(section);
            case "cierre" -> generateCierre(section);
            default -> "";
        };
    }

    @Override
    public String generateFact(FactTemplate template, FactNode node, Section section) {
        if (!"hecho".equals(template.id())) {
            return "";
        }
        String label = singleLine(node.getInputs().getOrDefault(FACT_LABEL_INPUT, ""));
        String narrative = singleLine(node.getInputs().getOrDefault("narrativa", ""));
        if (label.isEmpty()) {
            return narrative;
        }
        if (narrative.isEmpty()) {
            return label;
        }
        return label + ".- " + narrative;
    }

    @Override
    public String generateClosing(Section section) {
        List<String> lines = new ArrayList<>();
        appendFacts(section.getFacts(), lines);
        return String.join("\n", lines);
    }

    private void appendFacts(List<FactNode> nodes, List<String> lines) {
        for (FactNode node : nodes) {
            if (node.getKind() == FactNodeKind.FACT && !node.getContent().isBlank()) {
                lines.add(node.getContent());
            }
            appendFacts(node.getChildren(), lines);
        }
    }

    private String generateEncabezamiento(Section section) {
        String juzgado = singleLine(section.input("juzgado"));
        String demandante = singleLine(section.input("demandante"));
        String documento = singleLine(section.input("documento"));
        String domicilio = singleLine(section.input("domicilio"));
        String notificaciones = singleLine(section.input("notificaciones"));
        if (allBlank(juzgado, demandante, documento, domicilio, notificaciones)) {
            return "";
        }
        return "SEÑORA JUEZ DEL " + juzgado + ":\n"
                + demandante + ", con Documento Nacional de Identidad Nº " + documento
                + ", domiciliada en " + domicilio
                + "; y con domicilio para efectos de notificaciones en " + notificaciones
                + "; a usted atentamente digo:";
    }

    private String generateDemandado(Section section) {
        String demandado = singleLine(section.input("demandado"));
        String domicilio = singleLine(section.input("domicilioReal"));
        if (allBlank(demandado, domicilio)) {
            return "";
        }
        return "Solicitando Tutela Jurisdiccional Efectiva dirijo la Presente Acción en contra de "
                + demandado + "; a quien se le deberá notificar en su domicilio real sito en "
                + domicilio + "; lugar donde deberá ser emplazado con la presente demanda y anexos, "
                + "con las formalidades y garantías de ley.";
    }

    private String generatePetitorio(Section section) {
        String proceso = singleLine(section.input("proceso"));
        String tipo = singleLine(section.input("tipoDemanda"));
        String demandado = singleLine(section.input("demandado"));
        String monto = singleLine(section.input("monto"));
        String profesion = singleLine(section.input("profesion"));
        String alimentistas = joinList(section.input("alimentistas"));
        if (allBlank(proceso, tipo, demandado, monto, profesion, alimentistas)) {
            return "";
        }
        return "Recurro a su despacho a fin de interponer en vía de " + proceso
                + ", formal " + tipo + " en contra de " + demandado
                + "; a fin de que acuda con una pensión alimenticia en forma mensual y adelantada "
                + "en la suma de " + monto + "; que percibe en su calidad de " + profesion
                + "; a favor de sus menores hijos: " + alimentistas
                + " de edad respectivamente; la misma que se sustenta en los fundamentos de hecho "
                + "y de derecho que pasaré a exponer:";
    }

    /** Cierre de la demanda: "Ciudad, fecha", firma y documento (formato de los ejemplos). */
    private String generateCierre(Section section) {
        String ciudad = singleLine(section.input("ciudad"));
        String fecha = singleLine(section.input("fecha"));
        String demandante = singleLine(section.input("demandante"));
        String documento = singleLine(section.input("documento"));
        if (allBlank(ciudad, fecha, demandante, documento)) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        String lugar = joinWithComma(ciudad, fecha);
        if (!lugar.isEmpty()) {
            lines.add(lugar);
        }
        if (!demandante.isEmpty()) {
            lines.add(demandante);
        }
        if (!documento.isEmpty()) {
            lines.add("DNI Nº " + documento);
        }
        return String.join("\n", lines);
    }

    private static String joinWithComma(String first, String second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return first + ", " + second;
    }

    private String generateJuridicos(Section section) {
        List<String> blocks = new ArrayList<>();
        for (CollectionItem item : section.getItems()) {
            List<String> articles = new ArrayList<>();
            for (String line : item.getContent().split("\n")) {
                String article = line.trim();
                if (!article.isEmpty()) {
                    articles.add("❖ " + article);
                }
            }
            if (articles.isEmpty() && item.getTitle() == null) {
                continue;
            }
            StringBuilder block = new StringBuilder();
            if (item.getTitle() != null && !item.getTitle().isBlank()) {
                block.append(item.getTitle().trim().toUpperCase(Locale.ROOT));
            }
            if (!articles.isEmpty()) {
                if (!block.isEmpty()) {
                    block.append('\n');
                }
                block.append(String.join("\n", articles));
            }
            blocks.add(block.toString());
        }
        return String.join("\n\n", blocks);
    }

    private static boolean allBlank(String... values) {
        for (String value : values) {
            if (!value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String singleLine(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }

    /** Une líneas de una lista de personas: "A; B y C". */
    private static String joinList(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String line : value.split("\n")) {
            String part = line.trim();
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        if (parts.size() == 2) {
            return parts.get(0) + " y " + parts.get(1);
        }
        String joined = String.join(", ", parts.subList(0, parts.size() - 1));
        return joined + " y " + parts.get(parts.size() - 1);
    }

    private static List<SectionSpec> buildSections() {
        List<SectionSpec> sections = new ArrayList<>();

        sections.add(SectionSpec.of("cabecera", SectionKind.SIMPLE_INPUT, "Cabecera", List.of(
                new InputSpec("expediente", "Expediente"),
                new InputSpec("especialista", "Especialista Legal"),
                new InputSpec("escrito", "Escrito"))));

        sections.add(SectionSpec.of("titulo", SectionKind.GENERATED_TEXT, "Título", List.of(
                new InputSpec("titulo", "Título"))));

        sections.add(SectionSpec.of("sumilla", SectionKind.GENERATED_TEXT, "Sumilla", List.of(
                new InputSpec("sumilla", "Sumilla", true))));

        sections.add(SectionSpec.of("encabezamiento", SectionKind.GENERATED_TEXT, "Encabezamiento", List.of(
                new InputSpec("juzgado", "Juzgado"),
                new InputSpec("demandante", "Nombres y apellidos del demandante"),
                new InputSpec("documento", "Número de documento"),
                new InputSpec("domicilio", "Domicilio", true),
                new InputSpec("notificaciones", "Domicilio para efectos de notificaciones", true))));

        sections.add(SectionSpec.of("demandado", SectionKind.GENERATED_TEXT, "DEMANDADO:", List.of(
                new InputSpec("demandado", "Nombres y apellidos del demandado"),
                new InputSpec("domicilioReal", "Domicilio real del demandado", true)))
                .withRomanNumbering());

        sections.add(SectionSpec.of("petitorio", SectionKind.GENERATED_TEXT, "PETITORIO", List.of(
                new InputSpec("proceso", "Proceso"),
                new InputSpec("tipoDemanda", "Tipo de demanda"),
                new InputSpec("demandado", "Nombres y apellidos del demandado"),
                new InputSpec("monto", "Monto de la pensión / pago solicitado"),
                new InputSpec("profesion", "Profesión u ocupación del demandado", true),
                new InputSpec("alimentistas", "Hijos / alimentistas", true)))
                .withRomanNumbering());

        sections.add(SectionSpec.of("hechos", SectionKind.FACTS, "FUNDAMENTOS FÁCTICOS:", List.of())
                .withRomanNumbering()
                .withFacts(List.of(new FactTemplate("hecho", "Hecho", List.of(
                        new InputSpec(FACT_LABEL_INPUT, "Rótulo del hecho", true),
                        new InputSpec("narrativa", "Narrativa del hecho", true))))));

        sections.add(SectionSpec.of("juridicos", SectionKind.COLLECTION, "FUNDAMENTOS JURÍDICOS:", List.of())
                .withRomanNumbering()
                .withCollection(CollectionFormat.LEGAL_GROUP, true));

        sections.add(SectionSpec.of("representacion", SectionKind.EDITABLE_TEXT,
                        "REPRESENTACIÓN PROCESAL, LEGITIMIDAD E INTERÉS PARA OBRAR:", List.of())
                .withRomanNumbering());

        sections.add(SectionSpec.of("monto", SectionKind.EDITABLE_TEXT, "MONTO DEL PETITORIO:", List.of())
                .withRomanNumbering());

        sections.add(SectionSpec.of("via", SectionKind.EDITABLE_TEXT, "VÍA PROCEDIMENTAL Y COMPETENCIA:", List.of())
                .withRomanNumbering());

        sections.add(SectionSpec.of("medios", SectionKind.COLLECTION, "MEDIOS PROBATORIOS Y ANEXOS:", List.of())
                .withRomanNumbering()
                .withCollection(CollectionFormat.EVIDENCE, false));

        sections.add(SectionSpec.of("otrosies", SectionKind.COLLECTION, "Otrosíes", List.of())
                .withCollection(CollectionFormat.OTROSIE, false));

        sections.add(SectionSpec.of("cierre", SectionKind.GENERATED_TEXT, "Cierre", List.of(
                new InputSpec("ciudad", "Ciudad"),
                new InputSpec("fecha", "Fecha"),
                new InputSpec("demandante", "Nombres y apellidos de la demandante"),
                new InputSpec("documento", "Documento de identidad de la demandante"))));

        return sections;
    }
}
