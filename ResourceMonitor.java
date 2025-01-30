package mts;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
//Aceasta clasa monitorizeaza utilizarea procesorului si a memoriei.
public class ResourceMonitor {
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    public ResourceMonitor() {
        osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        memoryBean = ManagementFactory.getMemoryMXBean();
    }

    public double getCpuLoad() {
        return osBean.getProcessCpuLoad() * 100; // Se returneaza procentul
    }

    public long getUsedMemory() {
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        return heapMemoryUsage.getUsed(); // Returneaza memoria heap utilizata in bytes
    }

    public long getCommittedMemory() {
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        return heapMemoryUsage.getCommitted(); // Memorie heap alocata in bytes
    }
    
}
