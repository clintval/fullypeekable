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

import scala.collection.{AbstractIterator, BufferedIterator, mutable}

/** An iterator that is capable of buffering forward any number of elements. */
trait FullyPeekableIterator[+A] extends BufferedIterator[A] {

  /** Lift an element from this iterator without advancing the iterator. Stores peeked elements in memory. */
  def lift(index: Int): Option[A]

  /** Lift many elements from this iterator without advancing the iterator. Stores peeked elements in memory. */
  def liftMany(start: Int, end: Int): Seq[Option[A]]

  /** Peek elements while the predicate remains true. */
  def peekWhile(p: A => Boolean): FullyPeekableIterator[A]

  /** Return this iterator as it is already buffered. */
  override def buffered: this.type = this
}

/** Companion object for [[FullyPeekableIterator]]. */
object FullyPeekableIterator {

  /** Additional methods on the base Scala iterator class. */
  implicit class FullyPeekableIteratorImpl[A](private val iter: Iterator[A]) {

    /** Create a fully peekable iterator from this iterator. */
    def fullyPeekable: FullyPeekableIterator[A] = {
      new AbstractIterator[A] with FullyPeekableIterator[A] {
        private val queue: mutable.Queue[A] = new mutable.Queue[A]()

        /** If this iterator has another item or not. */
        def hasNext: Boolean = queue.nonEmpty || iter.hasNext

        /** The next item in this iterator without advancing the iterator. */
        def head: A = headOption.getOrElse(Iterator.empty.next())

        /** Lift an element from this iterator without advancing the iterator. */
        override def headOption: Option[A] = lift(0)

        /** The known size of this iterator. If there is no known size, return <= -1. */
        override def knownSize: Int = {
          val innerSize = iter.knownSize
          if (innerSize >= 0) innerSize + queue.knownSize else innerSize
        }

        /** Lift an element from this iterator without advancing the iterator. */
        def lift(index: Int): Option[A] = {
          while (queue.length <= index + 1 && iter.hasNext) queue.enqueue(iter.next())
          queue.lift(index)
        }

        /** Lift many elements from this iterator without advancing the iterator. */
        def liftMany(start: Int, end: Int): Seq[Option[A]] = Range.inclusive(start, end).map(lift)

        /** Peek elements while the predicate remains true. */
        def peekWhile(p: A => Boolean): FullyPeekableIterator[A] = {
          val self: FullyPeekableIterator[A] = this
          new AbstractIterator[A] {
            private var index: Int = 0
            override def hasNext: Boolean = self.lift(index).exists(p)
            override def next(): A = {
              val _next = self.lift(index).getOrElse(Iterator.empty.next())
              index += 1
              _next
            }

          }.fullyPeekable
        }

        /** Returns an iterator over contiguous elements that match the predicate without discarding excess. */
        override def takeWhile(p: A => Boolean): FullyPeekableIterator[A] = {
          val self: FullyPeekableIterator[A] = this
          new AbstractIterator[A] {
            def hasNext: Boolean = self.headOption.exists(p)
            def next(): A = self.next()
          }.fullyPeekable
        }

        /** Drops items while they match the predicate without discarding excess. */
        override def dropWhile(p: A => Boolean): this.type = {
          while (headOption.exists(p)) next()
          this
        }

        /** The next element in the iterator which will advance the iterator. */
        def next(): A = if (queue.nonEmpty) queue.dequeue() else iter.next()
      }
    }
  }
}
