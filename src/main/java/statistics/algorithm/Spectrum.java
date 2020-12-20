package statistics.algorithm;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import statistics.entity.AbstractEntity;
import statistics.entity.Program;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

/**
 * Transform an entity with reference to its program to spectrum vector (ef, ep, nf, np), and then
 * to defect scores, based on different algorithm.
 */
public class Spectrum {

  double ef;
  double ep;
  double nf;
  double np;

  public Spectrum(Program program, AbstractEntity entity) {
    ef = entity.getTotalNumberOfFailedTests();
    ep = entity.getTotalNumberOfPassedTests();
    nf = program.getTotalFailedTests(entity.getClass());
    np = program.getTotalPassedTests(entity.getClass());
  }

  public static Spectrum create(AbstractEntity entity) {
    return new Spectrum(entity.getParentProgram(), entity);
  }

  public Double toTarantula() {
    return Math.round(nf) == 0 ? 0 : (ef / (ef + nf)) / ((ef / (ef + nf)) + (ep / (ep + np)));
  }

  public Double toOchiai() {
    return Math.round(nf) != 0 ? ef / Math.sqrt((ef + ep) * (ef + nf)) : 0;
  }

  public Double toJaccard() {
    return ef / (ef + ep + nf);
  }

  public Double toRussellRao() {
    return ef / (ef + ep + nf + np);
  }

  public Double toSorensenDice() {
    return 2 * ef / (2 * ef + ep + nf);
  }

  public Double toKulczynski1() {
    return ef / (nf + ep);
  }

  public Double toSimpleMatching() {
    return (ef + np) / (ef + ep + nf + np);
  }

  public Double toM1() {
    return (ef + np) / (nf + ep);
  }

  public Double toRogersTanimoto() {
    return (ef + np) / (ef + np + 2 * nf + 2 * ep);
  }

  public Double toHamming() {
    return ef + np;
  }

  public Double toOverlap() {
    return ef / Math.min(Math.min(ef, ep), nf);
  }

  public Double toOchiai2() {
    return ef * np / Math.sqrt((ef + ep) * (nf + np) * (ef + np) * (ep + nf));
  }

  public Double toWong1() {
    return ef;
  }

  public Double toAmple() {
    return Math.abs(ef / (ef + nf) - ep / (ep + np));
  }

  public Double toHamann() {
    return (ef + np - ep - nf) / (ef + ep + nf + np);
  }

  public Double toDice() {
    return 2 * ef / (ef + ep + nf);
  }

  public Double toKulczynski2() {
    return (1 / 2) * (ef / (ef + nf) + ef / (ef + ep));
  }

  public Double toSokal() {
    return (2 * ef + 2 * np) / (2 * ef + 2 * np + nf + ep);
  }

  public Double toM2() {
    return ef / (ef + np + 2 * nf + 2 * ep);
  }

  public Double toGoodman() {
    return (2 * ef - nf - ep) / (2 * ef + nf + ep);
  }

  public Double toEuclid() {
    return Math.sqrt(ef + np);
  }

  public Double toAnderberg() {
    return ef / (ef + 2 * ep + 2 * nf);
  }

  public Double toZoltar() {
    return ef / (ef + ep + nf + 10000 * nf * ep / ef);
  }

  public Double toWong2() {
    return ef - ep;
  }

  public Double toWong3() {
    double h = ep;
    if (ep > 2 && ep <= 10) {
      h = 2 + 0.1 * (ep - 2);
    } else if (ep > 10) {
      h = 2.8 + 0.01 * (ep - 10);
    }
    return ef - h;
  }
}
