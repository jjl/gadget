# inspector gadget

A tool for introspecting java classes and clojure namespaces

Handy for figuring out what you can do with a *thing* at a repl.

Written during a period without proper internet access.

Yes, it uses eval for now. Caveat emptor. Buyer beware.

## Usage

Scenario: you move flat and it will take two weeks to get internet. Disaster!

Gadget to the rescue!

```clojure
(require '[irresponsible.gadget :as g])

(-> java.lang.Byte g/inspect (g/summary {:shorten? true}) print)
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

`:shorten?` - shorten common namespaces (e.g. java.lang -> j.l)
`:private?` - include private and protected members in the output?

## License

Copyright (c) 2016 James Laver

MIT LICENSE

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
