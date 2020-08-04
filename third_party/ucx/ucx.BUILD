licenses(["notice"])

package(default_visibility = ["//visibility:public"])

lib_files = ["lib/libuct.so","lib/libucs.so","lib/libucp.so","lib/libucm.so","lib/libjucx.so"]

jar_files = ["lib/jucx-1.10.0.jar"]

out_files = jar_files + lib_files

genrule(
    name = "ucx-srcs",
    outs = out_files,
    local = 1,
    cmd = "\n".join([
        'export INSTALL_DIR=$$(pwd)/$(@D)',
        'export TMP_DIR=$$(mktemp -d -t ucx.XXXXXX)',
        'echo $$TMP_DIR',
        'echo $$INSTALL_DIR',
        'mkdir -p $$TMP_DIR',
        'cp -pLR $$(pwd)/external/ucx/* $$TMP_DIR',
        'cd $$TMP_DIR',
        './autogen.sh',
        './contrib/configure-release --prefix=$$INSTALL_DIR --with-java --enable-mt --disable-numa',
        'make -j 4; make install',
        'rm -rf $$TMP_DIR',
    ]),
)

filegroup(
    name = "ucx-jar-file",
    srcs = jar_files,
)

filegroup(
    name = "ucx-so-files",
    srcs = lib_files,
)


