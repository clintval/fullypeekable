# fullypeekable

[![Unit Tests](https://github.com/clintval/fullypeekable/actions/workflows/unit-tests.yml/badge.svg)](https://github.com/clintval/fullypeekable/actions/workflows/unit-tests.yml)
[![Coverage Status](https://codecov.io/gh/clintval/fullypeekable/branch/main/graph/badge.svg)](https://codecov.io/gh/clintval/fullypeekable)
[![Language](https://img.shields.io/badge/language-scala-c22d40.svg)](https://www.scala-lang.org/)

Peek an iterator for as far as you'd like, memory allowing!

![Pacific City, Oregon](.github/img/cover.jpg)

```scala
val original = Seq(1, 2, 3)
val peekable = original.iterator.fullyPeekable
peekable.liftMany(0, 3) should contain theSameElementsInOrderAs Seq(Some(1), Some(2), Some(3), None)
peekable.toSeq should contain theSameElementsInOrderAs original
```

#### If Mill is your build tool

```scala
ivyDeps ++ Agg(ivy"io.cvbio.collection::fullypeekable::1.0.0")
```

#### If SBT is your build tool

```scala
libraryDependencies += "io.cvbio.collection" %% "fullypeekable" % "1.0.0"
```
