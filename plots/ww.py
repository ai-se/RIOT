# vim: set filetype=python ts=2 sw=2 sts=2 expandtab:
import sys, re, traceback, time, random


def rseed(seed=1):
    random.seed(int(seed))


def shuffle(lst):
    random.shuffle(lst)
    return lst


def about(f):
    print("\n-----| %s |-----------------" % f.__name__)
    if f.__doc__:
        print("# " + re.sub(r'\n[ \t]*', "\n# ", f.__doc__))


TRY = FAIL = 0


def ok(f=None):
    global TRY, FAIL
    if f:
        try:
            TRY += 1;
            about(f);
            f();
            print("# pass");
        except:
            FAIL += 1;
            print(traceback.format_exc());
        return f
    else:
        print("\n# %s TRY= %s ,FAIL= %s ,%%PASS= %s" % (
            time.strftime("%d/%m/%Y, %H:%M:%S,"),
            TRY, FAIL,
            int(round((TRY - FAIL) * 100 / (TRY + 0.001)))))


def contains(all, some):
    return all.find(some) != -1


def kv(d):
    """Return a string of the dictionary,
       keys in sorted order,
       hiding any key that starts with '_'"""
    return '(' + ', '.join(['%s: %s' % (k, d[k])
                            for k in sorted(d.keys())
                            if k[0] != "_"]) + ')'


def isa(k, seen=None):
    assert isinstance(k, type), "superclass must be 'object'"
    seen = seen or set()
    if k not in seen:
        seen.add(k)
        yield k
        for sub in k.__subclasses__():
            for x in isa(sub, seen):
                yield x


class Thing(object):
    def __repr__(i):
        return i.__class__.__name__ + kv(i.__dict__)


class o(Thing):
    def __init__(i, **dic): i.__dict__.update(dic)

    def __getitem__(i, x): return i.__dict__[x]


# ---------------------------------------
def asLambda(i, txt):
    def methodsOf(i):
        return [s for s in i.__dir__() if s[0] is not "_"]

    for one in methodsOf(i):
        txt = re.sub(one, 'z.%s()' % one, txt)
    txt = "lambda z: " + txt
    code = eval(txt)
    # e.g. print("> ",code(i))


# ---------------------------------------
# <BEGIN>
class State(Thing):
    tag = ""

    def __init__(i, name, m):
        i.aaaa = 111
        i.name = name
        i._trans = []
        i.model = m

    def trans(i, gaurd, there):
        i._trans += [o(gaurd=gaurd, there=there)]

    def step(i):
        for j in shuffle(i._trans):
            if j.gaurd(i):
                print("now", j.gaurd.__name__)
                i.onExit()
                j.there.onEntry()
                return j.there
        return i

    def onEntry(i):
        print("arriving at %s" % i.name)

    def onExit(i):
        print("leaving %s" % i.name)

    def quit(i):
        return False


class Ocean(State):
    tag = "_^_"

    def whales(i): return random.random() < 0.1

    def atlantis(i): return random.random() < 0.01


class Happy(State):
    tag = ":-)"

    def onEntry(i):
        print("i am so happy!")


class Sad(State):
    tag = ":-("

    def onEntry(i):
        print("i am so sad!")


class Exit(State):
    tag = "."

    def quit(i):
        return True

    def onExit(i):
        print("bye bye")
        return i


# ---------------------------------------
class Machine(Thing):
    """Maintains a set of named states.
       Creates new states if its a new name.
       Returns old states if its an old name."""

    def __init__(i, name, most=64):
        i.all = {}
        i.name = name
        i.start = None
        i.most = most

    def isa(i, x):
        if isinstance(x, State):
            return x
        for k in isa(State):
            if k.tag and contains(x, k.tag):
                return k(x, i)
        return State(x, i)

    def state(self, x):
        self.all[x] = y = self.all[x] if x in self.all else self.isa(x)
        self.start = self.start or y
        return y

    def trans(i, here, gaurd, there):
        i.state(here).trans(gaurd,
                            i.state(there))

    def run(i):
        print("booting %s" % i.name)
        state = i.start
        state.onEntry()
        for i in range(i.most):
            state = state.step()
            if state.quit():
                break
        return state.onExit()

    def maybe(i, s):
        return random.random() < 0.5

    def true(i, s):
        return True


class MyMachine(Machine):
    def rain(i, s): return random.random() < 0.3

    def sunny(i, s): return random.random() < 0.6

    def sick(i, s): return random.random() < 0.2


# ---------------------------------------
def make(m, fun):
    fun(m, m.state, m.trans)
    return m


# END>
# ---------------------------------------
# @ok
def machine1():
    m = Machine("main")
    s = m.state
    a = s("start")
    x = s("cheery:-)")
    y = s("crying:-(")
    e = s("sleeping.")
    a.trans(x.true, x)
    x.trans(x.rain, y)
    x.trans(x.sick, y)
    x.trans(x.maybe, e)
    y.trans(y.sunny, x)
    m.run()


def spec001(m, s, t):
    grin = s("cheery:-)")
    cry = s("crying:-(")
    t("start", m.true, grin)
    t(grin, m.rain, cry)
    t(grin, m.sick, cry)
    t(grin, m.maybe, "sleeping.")
    t(cry, m.sunny, grin)


@ok
def machine2():
    make(MyMachine("playtime"),
         spec001).run()


# ---------------------------------------
if __name__ == "__main__":
    # rseed(sys.argv[-1])
    rseed(1)
    ok()
