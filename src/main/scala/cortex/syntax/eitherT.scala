package cortex.syntax

import cats.Applicative
import cats.data.EitherT

extension (eitherT: EitherT.type)
  def condF[F[_]: Applicative, A](
    test: Boolean,
    left: A
  ): EitherT[F, A, Unit] =
    EitherT.cond[F](test, (), left)
