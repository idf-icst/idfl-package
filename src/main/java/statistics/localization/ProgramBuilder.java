package statistics.localization;

import statistics.entity.Program;
import statistics.profiling.ProfilingParser;

import java.util.Optional;

public class ProgramBuilder {

  ProfilingParser profilingParser;

  public ProgramBuilder setProfilingParser(ProfilingParser parser) {
    this.profilingParser = parser;
    return this;
  }

  public Optional<Program> build() {
    return profilingParser.toProgram();
  }


}
