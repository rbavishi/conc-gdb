### Conc-GDB ###

* Conc-GDB is a GDB-like debugger for Java Programs. It allows you to *break*, *continue* threads by their ID. It also allows you to take a single step using the *next* functionality.

* Conc-GDB is most useful for forcing a schedule that can expose concurrency bugs.

* A special feature is that of a **replay**. If you have found a bug by manually inducing a tricky schedule, Conc-GDB stores your commands, and allows you to replay them. This produces the exact schedule that you had used to find the bug. This is very useful for bug reports.

#### Installation ####

* Conc-GDB depends on [Soot](https://github.com/Sable/soot) and hence needs JRE 1.7 to function correctly.

* You need to modify the Makefile according to the path of your JRE-JVM installation.

#### Usage ####

* You can have a look at the PDF report for this tool, present in the repo itself.

#### Comments and Maintenance ####

* This tool was built as part of an assignment for a course. It is quite inefficient and I discourage its use in any serious setting.

* I will not be actively maintaining this. Forks are welcome.
