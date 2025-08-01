package stryker4s.report.dashboard

import cats.Monad
import cats.data.{OptionT, ValidatedNec}
import cats.effect.std.Env
import cats.syntax.all.*
import stryker4s.config.Config
import stryker4s.report.dashboard.Providers.*
import stryker4s.report.model.DashboardConfig

trait DashboardConfigProvider[F[_]] {
  def resolveConfig(): F[ValidatedNec[String, DashboardConfig]]
}

object DashboardConfigProvider {
  def apply[F[_]: Monad: Env]()(implicit config: Config): DashboardConfigProvider[F] =
    new DashboardConfigProviderImpl[F]()
}

private class DashboardConfigProviderImpl[F[_]: Monad: Env]()(implicit config: Config)
    extends DashboardConfigProvider[F] {

  def resolveConfig(): F[ValidatedNec[String, DashboardConfig]] =
    (resolveapiKey(), resolveproject(), resolveversion()).mapN { case t =>
      t.mapN {
        DashboardConfig(
          _,
          config.dashboard.baseUrl,
          config.dashboard.reportType,
          _,
          _,
          config.dashboard.module
        )
      }
    }

  private val apiKeyName = "STRYKER_DASHBOARD_API_KEY"
  private def resolveapiKey() =
    Env[F]
      .get(apiKeyName)
      .map(_.toValidNec(apiKeyName))

  private def resolveproject() =
    config.dashboard.project.toOptionT
      .orElse(byCiProvider(_.determineProject()))
      .value
      .map(_.toValidNec("dashboard.project"))

  private def resolveversion() =
    config.dashboard.version.toOptionT
      .orElse(byCiProvider(_.determineVersion()))
      .value
      .map(_.toValidNec("dashboard.version"))

  private def byCiProvider[T](f: CiProvider[F] => OptionT[F, T]) =
    Providers.determineCiProvider[F]().flatMap(f)
}
