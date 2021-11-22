// PART OF THE MACHINE SIMULATION. DO NOT CHANGE.

package nachos.machine;

/**
 * A single translation between a virtual page and a physical page.
 */
public final class TranslationEntry {
    /**
     * Allocate a new invalid translation entry.
     */
    public TranslationEntry() {
        valid = false;
    }

    /**
     * Allocate a new translation entry with the specified initial state.
     *
     * @param vpn      the virtual page number.
     * @param ppn      the physical page number.
     * @param valid    the valid bit.
     * @param readOnly the read-only bit.
     * @param used     the used bit.
     * @param dirty    the dirty bit.
     */
    public TranslationEntry(int vpn, int ppn, boolean valid, boolean readOnly,
                            boolean used, boolean dirty) {
        this.vpn = vpn;
        this.ppn = ppn;
        this.valid = valid;
        this.readOnly = readOnly;
        this.used = used;
        this.dirty = dirty;
    }

    /**
     * Allocate a new translation entry, copying the contents of an existing
     * one.
     *
     * @param entry the translation entry to copy.
     */
    public TranslationEntry(TranslationEntry entry) {
        vpn = entry.vpn;
        ppn = entry.ppn;
        valid = entry.valid;
        readOnly = entry.readOnly;
        used = entry.used;
        dirty = entry.dirty;
    }

    /**
     * The virtual page number.
     * 虚拟页页号
     */
    public int vpn;

    /**
     * The physical page number.
     * 物理页页号
     */
    public int ppn;

    /**
     * If this flag is <tt>false</tt>, this translation entry is ignored.
     * 如果此标志为false ，则忽略此翻译entry。
     */
    public boolean valid;

    /**
     * If this flag is <tt>true</tt>, the user program is not allowed to
     * modify the contents of this virtual page.
     * 如果此标志为true ，则不允许用户程序修改此虚拟页面的内容。
     */
    public boolean readOnly;

    /**
     * This flag is set to <tt>true</tt> every time the page is read or written
     * by a user program.
     * 每次用户程序读取或写入页面时，将此标志设置为真。
     */
    public boolean used;

    /**
     * This flag is set to <tt>true</tt> every time the page is written by a
     * user program.
     * 每次用户程序写入页面时，将此标志设置为真。
     */
    public boolean dirty;

    @Override
    public String toString() {
        return "TranslationEntry{" +
                "vpn=" + vpn +
                ", ppn=" + ppn +
                '}';
    }
}
