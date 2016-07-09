The irresponsible clojure guild present...

# inspector gadget

A tool for introspecting java classes.

Handy for figuring out what you can do with a *thing* at a repl.

![logo](https://github.com/irresponsible/tv100/blob/master/logo.png)

## Usage

Scenario: you move flat and it will take two weeks to get internet. Disaster!

Gadget to the rescue. Sadly I had to write it while I didn't have much internet.

[![Clojars Project](http://clojars.org/irresponsible/gadget/latest-version.svg)](http://clojars.org/irresponsible/gadget)

[![Travis CI](https://travis-ci.org/irresponsible/gadget.svg?branch=master)](https://travis-ci.org/irresponsible/gadget)


```clojure
boot.user> (require '[irresponsible.gadget :refer [wtf?]])

boot.user> (wtf? [])
(deftype++ clojure.lang.PersistentVector
  :bases [c.l.APersistentVector c.l.IEditableCollection c.l.IKVReduce c.l.IObj c.l.IReduce]
  ;; methods
  (^PersistentVector$Node .access$000 [self j.u.concurrent.atomic.AtomicReference int c.l.PersistentVector$Node])
  (^PersistentVector .adopt [self j.l.Object<>])
  (^Object<> .arrayFor [self int])
  (^ITransientCollection .asTransient [self])
  (^PersistentVector$TransientVector .asTransient [self])
  (^IPersistentVector .assocN [self int j.l.Object])
  (^PersistentVector .assocN [self int j.l.Object])
  (^IChunkedSeq .chunkedSeq [self])
  (^IPersistentCollection .cons [self j.l.Object])
  (^IPersistentVector .cons [self j.l.Object])
  (^PersistentVector .cons [self j.l.Object])
  (^int .count [self])
  (^PersistentVector .create [self c.l.IReduceInit]
                             [self c.l.ISeq]
                             [self j.l.Iterable]
                             [self j.l.Object<>]
                             [self j.u.List])
  (^:private ^PersistentVector$Node .doAssoc [self int c.l.PersistentVector$Node int j.l.Object])
  (^IPersistentCollection .empty [self])
  (^Iterator .iterator [self])
  (^Object .kvreduce [self c.l.IFn j.l.Object])
  (^IPersistentMap .meta [self])
  (^:private ^PersistentVector$Node .newPath [self j.u.concurrent.atomic.AtomicReference int c.l.PersistentVector$Node])
  (^Object .nth [self int]
                [self int j.l.Object])
  (^IPersistentStack .pop [self])
  (^PersistentVector .pop [self])
  (^:private ^PersistentVector$Node .popTail [self int c.l.PersistentVector$Node])
  (^:private ^PersistentVector$Node .pushTail [self int c.l.PersistentVector$Node c.l.PersistentVector$Node])
  (^Iterator .rangedIterator [self int int])
  (^Object .reduce [self c.l.IFn]
                   [self c.l.IFn j.l.Object])
  (^ISeq .seq [self])
  (^int .tailoff [self])
  (^IObj .withMeta [self c.l.IPersistentMap])
  (^PersistentVector .withMeta [self c.l.IPersistentMap])
  ;; fields
  (^PersistentVector .-EMPTY)
  (^c.l.PersistentVector$Node .-EMPTY_NODE)
  (^c.l.PersistentVector$Node .-root)
  (^int .-shift)
  (^j.l.Object<> .-tail))
nil
```
## Options

`wtf?` can take a second argument, a map of options:

* `:shorten?` - shorten common namespaces (e.g. java.lang -> j.l)
* `:private?` - include private and protected members in the output?

## API

There is an API, but until I've finished writing documental, you'll have to read the doc comments

`gadget`, `inspect` and `summary` are the only functions you'll need for repl work.

## Plans

At some point we'll probably want to parse java so we can get comments.

There are no plans to extend this analysis to clojure

## Contributions

Pull requests and issues welcome, even if it's just doc fixes. We don't bite.

## License

Copyright (c) 2016 James Laver

MIT LICENSE

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
