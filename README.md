# Introduction
This project contains code that implements and runs `instant dfl (IFL)` experiment. Two main packages, `defects4j` is for running open-source defects4j projects, 
and `statistics` is for running close-source project which relies on Clover for collecting coverage and spectrum.  

# Code Structure
- statistics.algorithm: This package is a collection of 25 spectrum-based ranking algorithms
- statistics.entity: This package captures data structures used in the experiment, such as, `TestCase`, `Program` under test, `ExecutionEntity`, etc.
- statistics.experiment: This package contains `glue code` to set up the experiment, such as, generating bash scripts to run the experiment in batch mode, calculating running time, composing
and reducing results in raw ranking score in json into relative rankings, TOP1, TOP5, MRP, etc.
- statistics.localizing: This is main package that actually takes in a program, and then outputs the ranking scores.
- statistics.profiling: This package allows spectrum data in a standardized format to be extracted from some instrumenting tools, such as, `mvn clover`.
- statistics.validate: Code to inspect a specific bug run.
- defects4j: this code package is for running experiments specifically for defects4j subjects

# For close-source projects:
## Project under evaluation
- Cloud-based event registration
- 28 injected bugs
- 13 real bugs
- injected bug info
    1. negate filter condition in a lambda expression of predicate type
    2. change the order of ? : operator
    3. change the initialized value from 0 to 1
    4. change max to min
    5. change initialized value from false to true
    6. if (hasGroupReg && newGroupMemberCount > numberNewGroupMembersAllowed) => if (hasGroupReg && newGroupMemberCount < numberNewGroupMembersAllowed)
    7. Change for loop exit condition from < to <=
    8. Change for loop exit condition from < to <=
    9. Changed initialized value assigned to a variable, 1900 to 1000
    10. Change method signature order: switch order between: boolean allowImportBypass and boolean allowPlannerBypass
    11. Modify if condition by removing negate operator
    12. Change predicate logic operator in lambda filter function: || to &&
    13. Change control flow in for loop by modifying critical if condition: == => !=
    14. Change the return value of a if-branch: true to false
    15. Change if condition: removing ! operator
    16. Change initialized value of a final variable: 100 to 1000
    17. Mutate if branch condition by changing && to || operator
    18. Mutate an expression by changing + to - operator
    19. Mutate if condition logic by changing < to >
    20. Mutate if condition logic by changing && to ||
    21. Mutate if condition logic by changing > to < operator
    22. Mutate if condition logic by changing && to || operator
    23. Mutate if condition logic by changing > to < operator
    24 Mutate if condition logic by changing > to < operator: numberOfGuests > minAllowed => numberOfGuests < minAllowed
    25. change && to ||
    26. negate an operator, change ! to !!
    27. change comparator > to <
    28. change operator + to -
- real bugs info
    1. Variable usage: mistakenly used wrong variable
    2. logic/devide by zero
    3. Boolean: incorrect lower bound in if condition
    4. Corner case: fee = 0 resulting in incoorect tax percentage calculation
    5. Exception: Failed to check IndexOutOfBoundsException
    6. Exception:Failed to check NullPointerException
    7. Exception: Failed to check NullPointerException
    8. Boolean: Incorrect filter condition in a lambda used in a long stream
    9. Calculation: Incorrect refund calculation for certain items
    10. Calculation: Incorrect discount calculation
    11. Calculation: Incorrect tax calculation 
    12. DateTime: problematic payment date time format 
    13. Boolean: Failed to check NullPointerException
## Running Workflow
For a close-source project, we rely on Clover to get spectrum data. Thus, we in order to run the experiment with it, we must first use Clover and Maven
to instrument and execute all the tests. Then configure this tool pointing `spectrum-dir` to the generated coverage results. 

    1. Build the main program into an executable jar file (specified in the src/main/resources/MANIFEST.MF)
    2. Use `experiment` setup code to generate setting scripts for different experimenting modes, such as, time-based, extra passes, extra fails, quadrant order, etc.
    3. Run the scripts in batch mode (this will take hours)
    4. The scripts will output raw data in json format (extremely large data for all settings)
    5. Use code in `experiment` to compose and reduce into human-readable format (e.g, csv)

# For Open-source Defects4J projects
The spectrum data are pre-collected and made available at: 
- http://fault-localization.cs.washington.edu/ 
- https://bitbucket.org/rjust/fault-localization-data/src/master/
- For more information: https://github.com/rjust/defects4j  
## Projects under evaluation
- Chart: 10 real bugs
- Lang: 14 real bugs
- Math: 25 real bugs
- Mockito: 8 real bugs 
## Running Workflow
    1. `cd to the main idfl-package folder that conains the pom.xml file`
    2. `mvn clean install -DskipTests` => to make sure everything compiles and jar files are created
    3. `cd to src/main/resources/defects4j`
    4. `git clone https://bitbucket.org/rjust/fault-localization-data.git`
    5. `wget --recursive --no-parent --accept gzoltar-files.tar.gz http://fault-localization.cs.washington.edu/data` => to download the subject dataset from the Washington university
    6. `mvn exec:java -Dexec.mainClass="defects4j.Configs" -Dexec.classpathScope=runtime` => to generate data processing script
    7. `cd to src/main/resources/scripts`
    8. `./unzipD4jDs.sh` to unzip all spectrum data
    9. `mvn exec:java -Dexec.mainClass="defects4j.Evaluation" -Dexec.classpathScope=runtime` => to run experiments and analyze results
## Results Summary
The experiment results are summarized and saved in the **new-experiment** folder, in which we will find 2 following files:
- D4J_Results_Summary.xlsx, which contains the following tabs:
    - IFL_1 vs IFL_c: presenting results of IFL_1 and IFL_c in summary 
    - Extra Failed Test Series: presenting results of IFL with first and second failed tests, across 8 D4J bugs that have at least 2 failed tests
    - Extra Passed Test Series: presenting results of IFL with 1, 2, 3, ..., 10 extra passed tests after the first failed test, across 39 D4J bugs that have at least 10 extra passed tests after the first failed test
    - IFL_1: presenting results of IFL_1 in all 25 algorithms
    - IFL_c: presenting results of IFL_c in all 25 algorithms 
 
# Main Dependencies
- `maven` (for building and executing tests)
- `clover` (for collecting spectrum-based data)
  
  

