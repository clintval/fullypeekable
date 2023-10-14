/*
 * The MIT License
 *
 * Copyright © 2023 Clint Valentine
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.cvbio.collection

import io.cvbio.collection.FullyPeekableIterator.FullyPeekableIteratorImpl
import org.scalatest.OptionValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/** Unit tests for [[FullyPeekableIterator]]. */
class FullyPeekableIteratorTest extends AnyFlatSpec with OptionValues with Matchers {

  "FullyPeekableIterator" should "make an iterator fully peekable when used as a trait" in {
    class FullyPeekable(iterator: Iterator[Int]) extends FullyPeekableIterator[Int] {
      private val underlying = iterator.fullyPeekable
      override def lift(index: Int): Option[Int] = underlying.lift(index)
      override def liftMany(start: Int, end: Int): Seq[Option[Int]] = underlying.liftMany(start, end)
      override def peekWhile(p: Int => Boolean): FullyPeekableIterator[Int] = underlying.peekWhile(p)
      override def head: Int = underlying.head
      override def hasNext: Boolean = underlying.hasNext
      override def next(): Int = underlying.next()
    }
    val expected = Seq(1, 2, 3)
    val peekable = new FullyPeekable(expected.iterator)
    peekable.liftMany(0, 3) should contain theSameElementsInOrderAs Seq(Some(1), Some(2), Some(3), None)
    peekable.toSeq should contain theSameElementsInOrderAs expected
    peekable.isEmpty shouldBe true
  }

  "FullyPeekableIteratorImpl" should "iterate over elements like a normal iterator" in {
    val expected = Seq(1, 2, 3)
    val peekable = expected.iterator.fullyPeekable
    peekable.toSeq should contain theSameElementsInOrderAs expected
  }

  it should "also support operations that a normal buffered iterator has (without advancing)" in {
    val expected = Seq(1, 2, 3)
    val peekable = expected.iterator.fullyPeekable
    peekable.headOption.value shouldBe 1
    peekable.head shouldBe 1
    peekable.toSeq should contain theSameElementsInOrderAs Seq(1, 2, 3)
  }

  it should "lift a single element from the head of the iterator without advancing" in {
    val expected = Seq(1, 2, 3)
    val peekable = expected.iterator.fullyPeekable
    peekable.lift(0).value shouldBe 1
    peekable.toSeq should contain theSameElementsInOrderAs expected
  }

  it should "know it has a next element even if it has peeked that single element" in {
    val expected = Seq(1)
    val peekable = expected.iterator.fullyPeekable
    peekable.lift(0).value shouldBe 1
    peekable.hasNext shouldBe true
    peekable.next() shouldBe 1
    peekable.hasNext shouldBe false
  }

  it should "not have a known size when it wraps an iterator without a known size" in {
    val expected = Seq(1, 2, 3)
    val iterator = expected.iterator
    val peekable = iterator.fullyPeekable
    peekable.knownSize shouldBe iterator.knownSize
    peekable.headOption.value shouldBe 1 // Fills the underlying buffer with 1 element.
    peekable.knownSize shouldBe iterator.knownSize
  }

  it should "have a known size when it wraps an iterator with a known size" in {
    class SizedIterator[T](iter: Iterator[T]) extends Iterator[T] {
      private val forced            = iter.toSeq
      private var seen              = 0
      private val underlying        = forced.iterator
      override def knownSize: Int   = forced.length - seen
      override def hasNext: Boolean = underlying.hasNext
      override def next(): T        = {
        val _next = underlying.next()
        seen += 1
        _next
      }
    }

    val sized    = new SizedIterator(Seq(1, 2, 3).iterator)
    val peekable = sized.fullyPeekable
    peekable.knownSize shouldBe 3
    peekable.headOption.value shouldBe 1
    peekable.knownSize shouldBe 3
    peekable.next() shouldBe 1
    peekable.knownSize shouldBe 2
    peekable.next() shouldBe 2
    peekable.next() shouldBe 3
    peekable.knownSize shouldBe 0
  }

  it should "be able to lift many elements without advancing the iterator" in {
    val expected = Seq(1, 2, 3)
    val peekable = expected.iterator.fullyPeekable
    peekable.liftMany(0, 3) should contain theSameElementsInOrderAs Seq(Some(1), Some(2), Some(3), None)
    peekable.toSeq should contain theSameElementsInOrderAs expected
  }

  it should "be able to peek forward while a predicate is true without advancing the iterator" in {
    val expected = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val peekable = expected.iterator.fullyPeekable
    peekable.peekWhile(_ == 0).toSeq shouldBe empty
    peekable.peekWhile(_ <= 5).toSeq should contain theSameElementsInOrderAs Seq(1, 2, 3, 4, 5)
    peekable.next() shouldBe 1 // Advance past the first element.
    peekable.peekWhile(_ <= 5).toSeq should contain theSameElementsInOrderAs Seq(2, 3, 4, 5)
    peekable.toSeq should contain theSameElementsInOrderAs expected.drop(1)
  }

  it should "be able to take while a predicate is true without excessively advancing the iterator" in {
    val expected = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val peekable = expected.iterator.fullyPeekable
    peekable.takeWhile(_ == 0).toSeq shouldBe empty
    peekable.takeWhile(_ <= 5).toSeq should contain theSameElementsInOrderAs Seq(1, 2, 3, 4, 5)
    peekable.next() shouldBe 6 // Advance past the sixth element.
    peekable.takeWhile(_ => true).toSeq should contain theSameElementsInOrderAs Seq(7, 8, 9, 10)
  }

  it should "be able to drop while a predicate is true without excessively advancing the iterator" in {
    val expected = Seq(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val peekable = expected.iterator.fullyPeekable
    peekable.dropWhile(_ <= 5).toSeq should contain theSameElementsInOrderAs Seq(6, 7, 8, 9, 10)
  }
}
