package uk.me.lings.scalaguice

import com.google.inject._
import binder._

/**
 * Allows binding via type parameters. Mix into <code>AbstractModule</code>
 *  (or subclass) to allow using a type parameter instead of
 * <code>classOf[Foo]</code> or <code>new TypeLiteral[Bar[Foo]] {}</code>.
 * 
 * For example, instead of
 * {{{
 * class MyModule extends AbstractModule {
 *   def configure {
 *     bind(classOf[Service]).to(classOf[ServiceImpl]).in(classOf[Singleton])
 *     bind(classOf[CreditCardPaymentService])
 *     bind(new TypeLiteral[Bar[Foo]]{}).to(classOf[FooBarImpl])
 *     bind(classOf[PaymentService]).to(classOf[CreditCardPaymentService])
 *   }
 * }
 * }}}
 * use
 * {{{
 * class MyModule extends AbstractModule with ScalaModule {
 *   def configure {
 *     bind[Service].to[ServiceImpl].in[Singleton]
 *     bind[CreditCardPaymentService]
 *     bind[Bar[Foo]].to[FooBarImpl]
 *     bind[PaymentService].to[CreditCardPaymentService]
 *   }
 * }
 * }}}
 *
 * '''Note''' This syntax allows binding to and from generic types.
 * It doesn't currently allow bindings between wildcard types because the
 * Scala compiler doesn't currently create manifests for wildcards.
 */
trait ScalaModule extends AbstractModule {
  // should be:
  // this: AbstractModule =>
  // see http://lampsvn.epfl.ch/trac/scala/ticket/3564

  import ScalaModule._

  private def binderAccess = super.binder // shouldn't need super

  def bind[T: Manifest] = new ScalaAnnotatedBindingBuilder[T] {
     val self = binderAccess bind typeLiteral[T]
  }

}

object ScalaModule {
  import java.lang.annotation.{Annotation => JAnnotation}

  trait ScalaScopedBindingBuilder extends ScopedBindingBuilderProxy {
    def in[TAnn <: JAnnotation : ClassManifest] = self in annotation[TAnn]
  }

  trait ScalaLinkedBindingBuilder[T] extends ScalaScopedBindingBuilder 
    with LinkedBindingBuilderProxy[T] { outer =>
    def to[TImpl <: T : Manifest] = new ScalaScopedBindingBuilder {
      val self = outer.self to typeLiteral[TImpl]
    }
  }

  trait ScalaAnnotatedBindingBuilder[T] extends ScalaLinkedBindingBuilder[T]
    with AnnotatedBindingBuilderProxy[T] { outer =>
    def annotatedWith[TAnn <: JAnnotation : ClassManifest] = new ScalaLinkedBindingBuilder[T] {
      val self = outer.self annotatedWith annotation[TAnn]
    }
  }

}
