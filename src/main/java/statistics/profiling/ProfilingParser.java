package statistics.profiling;


import statistics.entity.Program;

import java.util.Optional;

public interface ProfilingParser {

  Optional<Program> toProgram();
}
