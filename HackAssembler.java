import java.io.*;
import java.util.*;

public class HackAssembler {

    private final Map<String, String> compCode;
    private final List<String> jumpCode;
    private final Map<String, Integer> definedSymbols;

    public HackAssembler() {
        compCode = new HashMap<>();
        compCode.put("0", "0101010");
        compCode.put("1", "0111111");
        compCode.put("-1", "0111010");
        compCode.put("D", "0001100");
        compCode.put("A", "0110000");
        compCode.put("!D", "0001101");
        compCode.put("!A", "0110001");
        compCode.put("-D", "0001111");
        compCode.put("-A", "0110011");
        compCode.put("D+1", "0011111");
        compCode.put("A+1", "0110111");
        compCode.put("D-1", "0001110");
        compCode.put("A-1", "0110010");
        compCode.put("D+A", "0000010");
        compCode.put("D-A", "0010011");
        compCode.put("A-D", "0000111");
        compCode.put("D&A", "0000000");
        compCode.put("D|A", "0010101");
        compCode.put("M", "1110000");
        compCode.put("!M", "1110001");
        compCode.put("-M", "1110011");
        compCode.put("M+1", "1110111");
        compCode.put("M-1", "1110010");
        compCode.put("D+M", "1000010");
        compCode.put("D-M", "1010011");
        compCode.put("M-D", "1000111");
        compCode.put("D&M", "1000000");
        compCode.put("D|M", "1010101");

        jumpCode = new ArrayList<>();
        jumpCode.add("");
        jumpCode.add("JGT");
        jumpCode.add("JEQ");
        jumpCode.add("JGE");
        jumpCode.add("JLT");
        jumpCode.add("JNE");
        jumpCode.add("JLE");
        jumpCode.add("JMP");

        definedSymbols = new HashMap<>();
        definedSymbols.put("SP", 0);
        definedSymbols.put("LCL", 1);
        definedSymbols.put("ARG", 2);
        definedSymbols.put("THIS", 3);
        definedSymbols.put("THAT", 4);
        definedSymbols.put("R0", 0);
        definedSymbols.put("R1", 1);
        definedSymbols.put("R2", 2);
        definedSymbols.put("R3", 3);
        definedSymbols.put("R4", 4);
        definedSymbols.put("R5", 5);
        definedSymbols.put("R6", 6);
        definedSymbols.put("R7", 7);
        definedSymbols.put("R8", 8);
        definedSymbols.put("R9", 9);
        definedSymbols.put("R10", 10);
        definedSymbols.put("R11", 11);
        definedSymbols.put("R12", 12);
        definedSymbols.put("R13", 13);
        definedSymbols.put("R14", 14);
        definedSymbols.put("R15", 15);
        definedSymbols.put("SCREEN", 0x4000);
        definedSymbols.put("KBD", 0x6000);
    }

    public List<String> translate(List<String> lines) {
        List<String> noComments = handleComments(lines);
        List<String> noSpaces = handleSpaces(noComments);
        List<String> noSymbols = handleSymbols(noSpaces);
        return handleInstructions(noSymbols);
    }

    private List<String> handleComments(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            int index = line.indexOf("//");
            if (index != -1) {
                line = line.substring(0, index);
            }
            result.add(line);
        }
        return result;
    }

    private List<String> handleSpaces(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                // Remove all whitespace characters
                String noSpace = trimmed.replaceAll("\\s+", "");
                result.add(noSpace);
            }
        }
        return result;
    }

    private List<String> handleSymbols(List<String> lines) {
        // Create a copy of the defined symbols to work with
        Map<String, Integer> symbols = new HashMap<>(definedSymbols);
        List<String> result = new ArrayList<>();

        // First pass: record label declarations and build the result list without them.
        for (String line : lines) {
            if (line.startsWith("(") && line.endsWith(")")) {
                String symbol = line.substring(1, line.length() - 1);
                symbols.put(symbol, result.size());
            } else {
                result.add(line);
            }
        }

        // Second pass: replace variable symbols in A-instructions with their numeric addresses.
        int counter = 16;
        for (int i = 0; i < result.size(); i++) {
            String line = result.get(i);
            if (isAInstruction(line)) {
                String value = line.substring(1);
                try {
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    if (!symbols.containsKey(value)) {
                        symbols.put(value, counter);
                        counter++;
                    }
                    result.set(i, "@" + symbols.get(value));
                }
            }
        }
        return result;
    }

    private String translateDest(String line) {
        int result = 0;
        if (line.contains("=")) {
            String dest = line.split("=")[0];
            if (dest.contains("M")) {
                result |= 1;
            }
            if (dest.contains("D")) {
                result |= (1 << 1);
            }
            if (dest.contains("A")) {
                result |= (1 << 2);
            }
        }
        return String.format("%3s", Integer.toBinaryString(result)).replace(' ', '0');
    }

    private String translateComp(String line) {
        int eqIndex = line.indexOf("=");
        int scIndex = line.indexOf(";");
        int start = (eqIndex != -1) ? eqIndex + 1 : 0;
        int end = (scIndex != -1) ? scIndex : line.length();
        String comp = line.substring(start, end);
        return compCode.get(comp);
    }

    private String translateJump(String line) {
        String jump = "";
        if (line.contains(";")) {
            jump = line.split(";")[1];
        }
        int index = jumpCode.indexOf(jump);
        return String.format("%3s", Integer.toBinaryString(index)).replace(' ', '0');
    }

    private boolean isAInstruction(String line) {
        return line.startsWith("@");
    }

    private String translateAInstruction(String line) {
        int value = Integer.parseInt(line.substring(1));
        String binary = String.format("%15s", Integer.toBinaryString(value)).replace(' ', '0');
        return "0" + binary;
    }

    private String translateCInstruction(String line) {
        return "111" + translateComp(line) + translateDest(line) + translateJump(line);
    }

    private List<String> handleInstructions(List<String> lines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            if (isAInstruction(line)) {
                result.add(translateAInstruction(line));
            } else {
                result.add(translateCInstruction(line));
            }
        }
        return result;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java HackAssembler <filepath.asm>");
            return;
        }
        String filepath = args[0];
        if (!filepath.endsWith(".asm")) {
            System.err.println(filepath + " doesn't end with .asm");
            return;
        }
        String output = filepath.substring(0, filepath.lastIndexOf(".asm")) + ".hack";
        HackAssembler assembler = new HackAssembler();
        try {
            List<String> code = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(filepath));
            String line;
            System.out.println("Reading file: " + filepath);
            while ((line = reader.readLine()) != null) {
                code.add(line);
            }
            reader.close();

            List<String> translated = assembler.translate(code);
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            System.out.println("Writing output to: " + output);
            for (String outLine : translated) {
                writer.write(outLine);
                writer.newLine();
            }
            writer.close();
            System.out.println("Translation complete. Output saved to: " + output);
        } catch (IOException e) {
            System.err.println("Error processing file: " + e.getMessage());
        }
    }
}

