package stryker4s.command

import pureconfig.error.ConfigReaderException
import stryker4s.command.config.ProcessRunnerConfig
import stryker4s.config.ConfigReader._
import stryker4s.run.threshold.ErrorStatus

// TODO: We need a unified way of naming this starter class.
//  In the SBT module it's called Stryker4sPlugin which is not very descriptive to use here.
object AStarterClass extends App {

  Stryker4sArgumentHandler.handleArgs(args)

  private[this] val processRunnerConfig: ProcessRunnerConfig = {
    pureconfig.loadConfig[ProcessRunnerConfig](defaultConfigFileLocation.path, namespace = defaultNamespace) match {
      case Left(failures) => throw ConfigReaderException(failures)
      case Right(config)  => config
    }
  }

  val result = new Stryker4sCommandRunner(processRunnerConfig).run()

  val exitCode = result match {
    case ErrorStatus => 1
    case _           => 0
  }

  this.exit()

  private def exit(): Unit = {
    sys.exit(exitCode)
  }

}
