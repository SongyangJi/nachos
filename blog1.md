# nachos.conf

When Nachos starts, it reads in nachos.conf from the current
directory.  It contains a bunch of keys and values, in the simple
format "key = value" with one key/value pair per line. To change the
default scheduler, default shell program, to change the amount of
memory the simulator provides, or to reduce network reliability, modify
this file.

## Machine.stubFileSystem
Specifies whether the machine should provide a stub file system. A
stub file system just provides direct access to the test directory.
Since we're not doing the file system project, this should always
be true.

## Machine.processor
是否提供 MIPS的处理器
Specifies whether the machine should provide a MIPS processor. In
the first project, we only run kernel code, so this is false. In
the other projects it should be true.

## Machine.console
是否提供 console
Specifies whether the machine should provide a console. Again, the
first project doesn't need it, but the rest of them do.

## Machine.disk
是否提供一个模拟的硬盘
Specifies whether the machine should provide a simulated disk. No
file system project, so this should always be false.

## ElevatorBank.allowElevatorGUI
Normally true. When we grade, this will be false, to prevent
malicious students from running a GUI during grading.

## NachosSecurityManager.fullySecure
安全检查
Normally false. When we grade, this will be true, to enable
additional security checks.

## Kernel.kernel
指定加载那个内核(四个实验加载不同的 Kernel 类)
Specifies what kernel class to dynmically load. 
For proj1, this is nachos.threads.ThreadedKernel. 
For proj2, this should be nachos.userprog.UserKernel. 
For proj3, nachos.vm.VMKernel. 
For proj4, nachos.network.NetKernel.

## Processor.usingTLB
是否使用 TLB
Specifies whether the MIPS processor provides a page table
interface or a TLB interface. In page table mode (proj2), the
processor accesses an arbitrarily large kernel data structure to do
address translation. In TLB mode (proj3 and proj4), the processor
maintains a small TLB (4 entries).

## Processor.numPhysPages
物理页大小
The number of pages of physical memory.  Each page is 1K. This is
normally 64, but we can lower it in proj3 to see whether projects
thrash or crash.
