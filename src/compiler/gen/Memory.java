/**
 * @ Author: turk
 * @ Description: Emulator pomnilnika navideznega stroja.
 */

package compiler.gen;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import common.Constants;
import compiler.frm.Frame;

public class Memory {
    /**
     * Velikost pomnilnika v bajtih.
     */
    public final int size;

    /**
     * Emulator pomnilnika - preslikava iz naslovov v vrednosti poljubnih tipov.
     */
    private Map<Integer, Object> memory = new HashMap<>();

    /**
     * Začasne spremenljivke ('registri').
     */
    private Map<Frame.Temp, Object> temps = new HashMap<>();

    /**
     * Preslikava iz label (imenovanih lokacij) v naslove.
     */
    private Map<Frame.Label, Integer> labelToAddressMapping = new HashMap<>();

    public Memory(int size) {
        this.size = size;
    }

    /**
     * Na podan naslov shrani vrednost.
     */
    public void stM(int address, Object value) {
        validateAddress(address);
        memory.put(address, value);
    }

    /**
     * Na podano imenovano lokacijo shrani vrednost.
     */
    public void stM(Frame.Label label, Object value) {
        if (labelToAddressMapping.containsKey(label)) {
            memory.put(address(label), value);
        } else {
            throw new IllegalArgumentException("Unknown label!");
        }
    }

    /**
     * V podan register shrani vrednost.
     */
    public void stT(Frame.Temp temp, Object value) {
        temps.put(temp, value);
    }

    /**
     * Preberi vrednost iz podanega naslova.
     */
    public Object ldM(int address) {
        validateAddress(address);
        if (memory.containsKey(address)) {
            return memory.get(address);
        }
        throw new IllegalArgumentException("Empty address " + address + "!");
    }

    /**
     * Preberi vrednost iz podane poimenovane lokacije.
     */
    public Object ldM(Frame.Label label) {
        if (labelToAddressMapping.containsKey(label)) {
            var address = labelToAddressMapping.get(label);
            return memory.get(address);
        }
        throw new IllegalArgumentException("Empty address for label " + label.toString() + "!");
    }

    /**
     * Preberi vrednost iz podane začasne spremenljivke oz. registra.
     */
    public Object ldT(Frame.Temp temp) {
        if (temps.containsKey(temp)) {
            return temps.get(temp);
        }
        throw new IllegalArgumentException("Unknown temp " + temp.toString() + "!");
    }

    /**
     * Ustvari poimenovano lokacijo v pomnilniku.
     */
    public void registerLabel(Frame.Label label, int address) {
        validateAddress(address);
        labelToAddressMapping.put(label, address);
    }

    /**
     * Pridobi naslov za podano poimenovano lokacijo.
     */
    public int address(Frame.Label label) {
        return labelToAddressMapping.get(label);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        if (!temps.isEmpty())
            sb.append("Temps:\n");
        var tempsStr = temps.entrySet().stream()
            .sorted((o1, o2) -> o1.getKey().id - o2.getKey().id)
            .map(entry -> entry.getKey().toString() + ": " + entry.getValue().toString())
            .collect(Collectors.joining("\n"));
        sb.append(tempsStr);
        if (!tempsStr.isEmpty()) {
            sb.append("\n");
        }
        var memStr = memory.entrySet().stream()
            .sorted((o1, o2) -> o2.getKey() - o1.getKey())
            .map(entry -> entry.getKey().toString() + ": " + entry.getValue().toString())
            .collect(Collectors.joining("\n"));
        sb.append(memStr);
        return sb.toString();
    }

    private void validateAddress(int address) {
        // if debugMode
        if (address > size) {
            throw new IllegalArgumentException("Address " + address + " out of bounds!");
        }
        if (address == 0) {
            throw new IllegalArgumentException("Cannot dereference a null pointer!");
        }
        if (address % Constants.WordSize != 0) {
            throw new IllegalArgumentException("Address " + address + " not aligned!");
        }
        // endif
    }
}
