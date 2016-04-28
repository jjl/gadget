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
(require '[irresponsible.gadget :refer [wtf?]])

(wtf? java.lang.Byte)
;; output:
;; (deftype+ java.lang.Byte
;;   j.l.Comparable j.l.Number
;;   (Byte. [String])
;;   (^byte .byteValue [self])
;;   (^int .compare [self byte byte])
;;   (^int .compareTo [self j.l.Object] [self Byte])
;;   (^Byte .decode [self String])
;;   (^double .doubleValue [self])
;;   (^boolean .equals [self j.l.Object])
;;   (^float .floatValue [self])
;;   (^int .hashCode [self byte] [self])
;;   (^int .intValue [self])
;;   (^long .longValue [self])
;;   (^byte .parseByte [self String] [self String int])
;;   (^short .shortValue [self])
;;   (^String .toString [self] [self byte])
;;   (^int .toUnsignedInt [self byte])
;;   (^long .toUnsignedLong [self byte])
;;   (^Byte .valueOf [self byte] [self String] [self String int])
;;   (^int .-BYTES)
;;   (^byte .-MAX_VALUE)
;;   (^byte .-MIN_VALUE)
;;   (^int .-SIZE)
;;   (^j.l.Class .-TYPE))
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
