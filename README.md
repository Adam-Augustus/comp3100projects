S1 2023 COMP3100
DANIEL JOHNSTON sid:47095733

Github repo for Stage 1 of COMP3100 project:

This is not the weekly submission repo because that had strange branches everywhere and sub-repo's that i tried and failed to fix (at least i don't have time to figure it out before the deadline.)
That repo is available at:
https://github.com/Adam-Augustus/COMP3100

S1testConfigs removed 03/04/2023

For stage 2 of COMP3100 project:



First-Fit
Pros: easy to implement, doesn't have to search through whole server list to find a server
Cons: Won't choose the best server, can occupy too many resources unneccesarliy

Best-Fit
Pros: Schedules jobs to the most optimised server
Cons: Slower, has to calculate fitness for eash server for each task

Worst-Fit
Pros: Schedules fast, Lrr but uses all servers
Cons: Can schedule a large job to a small server, 

command to test effectiveness
./s2 test.py "java MyClient" -n -r results/ref results.json