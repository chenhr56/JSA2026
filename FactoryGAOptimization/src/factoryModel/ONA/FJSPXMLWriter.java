package factoryModel.ONA;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mitm.atb.SequenceDependentTaskInfo;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Converts Brandimarte FJSP benchmark data to ONA-compatible XML format.
 *
 * FJSP format (see DataSetExplanation.txt):
 *   Line 1: nJobs nMachines [avgMachinesPerOp]
 *   Each job line: nOps {k (machine time)...}...
 *
 * Mapping:
 *   FJSP machine  → processingDevice (all type="Small", availability=1)
 *   FJSP job      → productionProcess (instances=1)
 *   FJSP operation → subProcess
 *   machine alternatives → subProcessProcessingDevice entries
 */
public class FJSPXMLWriter {

    private static final int ENERGY_DEFAULT = 0;
    private static final int MONTARY_DEFAULT = 0;

    // ── Data model ──────────────────────────────────────────────────────

    private static final class FJSPInstance {
        int nJobs, nMachines;
        List<FJSPJob> jobs = new ArrayList<>();
    }

    private static final class FJSPJob {
        List<FJSPOperation> operations = new ArrayList<>();
    }

    private static final class FJSPOperation {
        /** Each entry is [machineIndex (0-based), processingTime] */
        List<int[]> alternatives = new ArrayList<>();
    }

    // ── Parser ──────────────────────────────────────────────────────────

    private static FJSPInstance parse(String filePath) throws IOException {
        // Read all tokens as strings first (header may contain floats like "3.5")
        List<String> rawTokens = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(filePath))) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;
            for (String part : trimmed.split("\\s+")) {
                if (!part.isEmpty())
                    rawTokens.add(part);
            }
        }

        FJSPInstance inst = new FJSPInstance();
        inst.nJobs = Integer.parseInt(rawTokens.remove(0));
        inst.nMachines = Integer.parseInt(rawTokens.remove(0));

        // The 3rd header token is avg machines/op — always present in
        // Brandimarte/Dauzere/Hurink FJSP benchmark data. Skip it.
        int startPos = 1;

        parseJobs(inst, rawTokens, startPos);
        return inst;
    }

    private static void parseJobs(FJSPInstance inst, List<String> tokens, int startPos) {
        int pos = startPos;
        for (int jobIdx = 0; jobIdx < inst.nJobs; jobIdx++) {
            int nOps = Integer.parseInt(tokens.get(pos++));
            FJSPJob job = new FJSPJob();

            for (int opIdx = 0; opIdx < nOps; opIdx++) {
                int k = Integer.parseInt(tokens.get(pos++));
                FJSPOperation op = new FJSPOperation();

                for (int m = 0; m < k; m++) {
                    int machine = Integer.parseInt(tokens.get(pos++));    // 1-based in data
                    int procTime = Integer.parseInt(tokens.get(pos++));
                    op.alternatives.add(new int[] { machine, procTime });
                }
                job.operations.add(op);
            }
            inst.jobs.add(job);
        }
    }

    // ── XML generation ──────────────────────────────────────────────────

    private static void convert(String inputPath, String outputPath) throws IOException {
        System.out.println("Parsing: " + inputPath);
        FJSPInstance inst = parse(inputPath);

        Document doc = new Document();
        Element root = new Element("ONAFactoryModel");
        doc.setRootElement(root);

        root.addContent(addObjectives());
        root.addContent(addProcessingDevices(inst.nMachines));
        root.addContent(addProductionProcesses(inst));
        root.addContent(new Element("sequenceDependentSetups"));

        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(doc, new FileWriter(outputPath));

        System.out.println("Written: " + outputPath + "  (jobs=" + inst.nJobs
                + ", machines=" + inst.nMachines + ")");
    }

    private static Element addObjectives() {
        Element objElement = new Element("objectives");
        for (String name : Arrays.asList("makespan", "montary")) {
            Element obj = new Element("objective");
            obj.setAttribute(new Attribute("name", name));
            objElement.addContent(obj);
        }
        return objElement;
    }

    private static Element addProcessingDevices(int nMachines) {
        Element devicesElement = new Element("processingDevices");

        for (int i = 1; i <= nMachines; i++) {
            Element device = new Element("processingDevice");
            device.setAttribute(new Attribute("name", "Small " + i));
            device.setAttribute(new Attribute("availability", "1"));

            Element unavailableTimes = new Element("unavailableTimes");
            unavailableTimes.addContent(new Element("unavailableTime"));
            device.addContent(unavailableTimes);

            devicesElement.addContent(device);
        }
        return devicesElement;
    }

    private static Element addProductionProcesses(FJSPInstance inst) {
        Element processesElement = new Element("productionProcesses");

        // Each FJSP operation becomes its own productionProcess so that
        // the subprocess's compatible device list matches the process-level
        // list (ONA model requires them to be the same).
        int priority = 1;
        for (int jobIdx = 0; jobIdx < inst.jobs.size(); jobIdx++) {
            FJSPJob job = inst.jobs.get(jobIdx);
            for (int opIdx = 0; opIdx < job.operations.size(); opIdx++) {
                FJSPOperation op = job.operations.get(opIdx);
                String processName = "P" + (jobIdx + 1);

                Element process = new Element("productionProcess");
                process.setAttribute(new Attribute("name", processName));
                process.setAttribute(new Attribute("instances", "1"));
                process.setAttribute(new Attribute("priority", String.valueOf(priority++)));
                process.setAttribute(new Attribute("cuts", "1"));

                // Process-level compitableDevices = this operation's machines
                Element compitables = new Element("comptiableDevices");
                for (int[] alt : op.alternatives) {
                    String devName = "Small " + alt[0];
                    Element compitable = new Element("comptiableDevice");
                    compitable.setAttribute(new Attribute("name", devName));
                    compitable.setAttribute(new Attribute("processingTime", String.valueOf(alt[1])));
                    compitable.setAttribute(new Attribute("energy", String.valueOf(ENERGY_DEFAULT)));
                    compitable.setAttribute(new Attribute("montary", String.valueOf(MONTARY_DEFAULT)));
                    compitables.addContent(compitable);
                }
                process.addContent(compitables);

                // Single subProcess with the same compatible machines
                Element subProcesses = new Element("subProcesses");
                Element subProcess = new Element("subProcess");
                subProcess.setAttribute(new Attribute("name", processName + " 0"));

                for (int[] alt : op.alternatives) {
                    Element spDevice = new Element("subProcessProcessingDevice");
                    spDevice.setAttribute(new Attribute("name", "Small " + alt[0]));
                    spDevice.setAttribute(new Attribute("processingTime", String.valueOf(alt[1])));
                    spDevice.setAttribute(new Attribute("energy", String.valueOf(ENERGY_DEFAULT)));
                    spDevice.setAttribute(new Attribute("montary", String.valueOf(MONTARY_DEFAULT)));
                    subProcess.addContent(spDevice);
                }
                subProcesses.addContent(subProcess);
                process.addContent(subProcesses);

                processesElement.addContent(process);
            }
        }
        return processesElement;
    }

    // ── Main ────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        String baseIn = "D:/code/FactoryOptimization/FactoryOptimization/input/benchmarkDataset/Dataset/Brandimarte_Data/Text/";
        String baseOut = "D:/code/FactoryOptimization/FactoryOptimization/input/";

        String[] files = { "Mk01", "Mk02", "Mk03", "Mk04", "Mk05",
                           "Mk06", "Mk07", "Mk08", "Mk09", "Mk10" };

        String[] outs = {"ONAConfiguration1", "ONAConfiguration2", "ONAConfiguration3", "ONAConfiguration4", "ONAConfiguration5",
                         "ONAConfiguration6", "ONAConfiguration7", "ONAConfiguration8", "ONAConfiguration9", "ONAConfiguration10" };

        for (int i = 0; i < outs.length; i++) {
            try {
                convert(baseIn + files[i] + ".fjs", baseOut + outs[i] + ".xml");
            } catch (IOException e) {
                System.err.println("Failed: " + outs[i] + " — " + e.toString());
            }
        }
    }
}
