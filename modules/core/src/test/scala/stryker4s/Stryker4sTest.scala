package stryker4s

import org.mockito.captor.ArgCaptor
import stryker4s.config.Config
import stryker4s.files.ConfigFilesResolver
import stryker4s.mutants.applymutants.ActiveMutationContext
import stryker4s.mutants.findmutants.{MutantFinder, MutantMatcherImpl}
import stryker4s.mutants.tree.{InstrumenterOptions, MutantCollector, MutantInstrumenter}
import stryker4s.mutants.{Mutator, TreeTraverserImpl}
import stryker4s.report.{AggregateReporter, FinishedRunEvent}
import stryker4s.run.threshold.SuccessStatus
import stryker4s.run.{MutantRunner, RollbackHandler}
import stryker4s.testkit.{FileUtil, LogMatchers, MockitoSuite, Stryker4sIOSuite}
import stryker4s.testutil.stubs.{TestFileResolver, TestProcessRunner, TestRunnerStub}

import scala.util.Success

class Stryker4sTest extends Stryker4sIOSuite with MockitoSuite with LogMatchers {

  test("should call mutate files and report the results") {
    val file = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val testFiles = Seq(file)
    val testSourceCollector = new TestFileResolver(testFiles)
    val testProcessRunner = TestProcessRunner(Success(1), Success(1), Success(1), Success(1))
    val reporterMock = mock[AggregateReporter]
    val rollbackHandler = mock[RollbackHandler]
    when(reporterMock.mutantTested).thenReturn(_.drain)
    whenF(reporterMock.onRunFinished(any[FinishedRunEvent])).thenReturn(())

    implicit val conf: Config = Config.default.copy(baseDir = FileUtil.getResource("scalaFiles"))

    val testMutantRunner =
      new MutantRunner(
        TestRunnerStub.resource,
        new ConfigFilesResolver(testProcessRunner),
        rollbackHandler,
        reporterMock
      )

    val sut = new Stryker4s(
      testSourceCollector,
      new Mutator(
        new MutantFinder(),
        new MutantCollector(new TreeTraverserImpl(), new MutantMatcherImpl()),
        new MutantInstrumenter(InstrumenterOptions.sysContext(ActiveMutationContext.sysProps))
      ),
      testMutantRunner,
      reporterMock
    )

    sut.run().asserting { result =>
      val runReportMock = ArgCaptor[FinishedRunEvent]
      verify(reporterMock).onRunFinished(runReportMock)
      val FinishedRunEvent(reportedResults, _, _, _) = runReportMock.value

      assertEquals(reportedResults.files.flatMap(_._2.mutants).size, 4)
      assertEquals(reportedResults.files.loneElement._1, "simpleFile.scala")
      assertEquals(result, SuccessStatus)
    }

  }
}