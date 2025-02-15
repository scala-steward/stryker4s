package stryker4s.report.mapper

import cats.syntax.option.*
import fs2.io.file.Path
import mutationtesting.*
import stryker4s.config.Config
import stryker4s.model.{MutantId, MutantMetadata, MutantWithId, MutatedCode}
import stryker4s.mutation.*
import stryker4s.testkit.{FileUtil, Stryker4sSuite}

import java.nio.file.Files
import scala.meta.{Lit, Term}
import scala.util.Properties

class MutantRunResultMapperTest extends Stryker4sSuite {
  describe("mapper") {
    test("should map 4 files to valid MutationTestResult") {
      val sut = new MutantRunResultMapper {}
      implicit val config: Config =
        Config.default.copy(thresholds = Config.default.thresholds.copy(high = 60, low = 40))

      val result = sut.toReport(mutationRunResults, testFiles.some)

      assertEquals(result.thresholds, Thresholds(high = 60, low = 40))
      assertEquals(result.files.size, 2)
      val firstResult = result.files.find(_._1.endsWith("scalaFiles/ExampleClass.scala")).value
      assert(result.files.exists(_._1.endsWith("scalaFiles/simpleFile.scala")))
      val FileResult(source, mutants, language) = firstResult._2
      assertEquals(language, "scala")
      assertEquals(
        mutants,
        List(
          MutantResult(
            "0",
            "EqualityOperator",
            "!=",
            Location(Position(4, 27), Position(4, 29)),
            MutantStatus.Killed
          ),
          MutantResult(
            "1",
            "StringLiteral",
            "\"\"",
            Location(Position(6, 31), Position(6, 37)),
            MutantStatus.Survived
          )
        )
      )
      assertEquals(
        source,
        new String(Files.readAllBytes(FileUtil.getResource("scalaFiles/ExampleClass.scala").toNioPath))
      )
      val framework = result.framework.value
      assertEquals(result.config.value, config)
      assertEquals(framework.name, "Stryker4s")
      assertEquals(framework.branding.value.homepageUrl, "https://stryker-mutator.io")
      assert(framework.branding.value.imageUrl.value.nonEmpty)

      val system = result.system.value
      assertEquals(system.ci, sys.env.contains("CI"))
      assertEquals(
        system.os.value,
        OSInformation(platform = Properties.osName, version = sys.props("os.version").some)
      )
      assertEquals(system.cpu.value, CpuInformation(logicalCores = Runtime.getRuntime().availableProcessors()))
      assertEquals(system.ram.value, RamInformation(total = Runtime.getRuntime().totalMemory() / 1024 / 1024))

    }

  }

  def mutationRunResults = {
    val path = FileUtil.getResource("scalaFiles/ExampleClass.scala")
    val mutantRunResult =
      toMutant(0, EqualTo.tree, NotEqualTo, path).toMutantResult(MutantStatus.Killed)

    val mutantRunResult2 =
      toMutant(1, Lit.String("Hugo"), EmptyString, path).toMutantResult(MutantStatus.Survived)

    val path3 = FileUtil.getResource("scalaFiles/simpleFile.scala")
    val mutantRunResult3 =
      toMutant(0, GreaterThan.tree, LesserThan, path3).toMutantResult(MutantStatus.Killed)

    Map(path -> Vector(mutantRunResult, mutantRunResult2), path3 -> Vector(mutantRunResult3))
  }

  def testFiles: TestFileDefinitionDictionary = Map.empty

  /** Helper method to create a [[stryker4s.model.MutantWithId]], with the `original` param having the correct
    * `Location` property
    */
  private def toMutant(id: Int, original: Term, category: SubstitutionMutation[? <: Term], file: Path) = {
    import stryker4s.extension.TreeExtensions.FindExtension

    import scala.meta.*
    val parsed = file.toNioPath.parse[Source]
    val foundOrig = parsed.get.find(original).value
    MutantWithId(
      MutantId(id),
      MutatedCode(
        foundOrig,
        MutantMetadata(foundOrig.syntax, category.tree.syntax, category.mutationName, foundOrig.pos, none)
      )
    )
  }
}
