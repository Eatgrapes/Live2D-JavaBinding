#ifdef _WIN32
#include <windows.h>
#include <GL/gl.h>
#include <stddef.h>
#include <stdio.h>

// Definitions from GLES2/gl2.h (simplified)
typedef char GLchar;
typedef ptrdiff_t GLsizeiptr;
typedef ptrdiff_t GLintptr;
typedef unsigned int GLbitfield;
typedef int GLsizei;
typedef int GLint;
typedef unsigned int GLuint;
typedef unsigned int GLenum;
typedef unsigned char GLboolean;
typedef float GLfloat;
typedef float GLclampf;

// Pointers
typedef void (APIENTRY* PFN_glGenFramebuffers)(GLsizei n, GLuint* framebuffers);
typedef void (APIENTRY* PFN_glBindFramebuffer)(GLenum target, GLuint framebuffer);
typedef void (APIENTRY* PFN_glFramebufferTexture2D)(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
typedef void (APIENTRY* PFN_glDeleteFramebuffers)(GLsizei n, const GLuint* framebuffers);
typedef GLenum (APIENTRY* PFN_glCheckFramebufferStatus)(GLenum target);
typedef void (APIENTRY* PFN_glActiveTexture)(GLenum texture);
typedef void (APIENTRY* PFN_glBlendFuncSeparate)(GLenum sfactorRGB, GLenum dfactorRGB, GLenum sfactorAlpha, GLenum dfactorAlpha);
typedef GLuint (APIENTRY* PFN_glCreateShader)(GLenum type);
typedef void (APIENTRY* PFN_glShaderSource)(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length);
typedef void (APIENTRY* PFN_glCompileShader)(GLuint shader);
typedef void (APIENTRY* PFN_glGetShaderiv)(GLuint shader, GLenum pname, GLint* params);
typedef void (APIENTRY* PFN_glGetShaderInfoLog)(GLuint shader, GLsizei bufSize, GLsizei* length, GLchar* infoLog);
typedef void (APIENTRY* PFN_glDeleteShader)(GLuint shader);
typedef GLuint (APIENTRY* PFN_glCreateProgram)(void);
typedef void (APIENTRY* PFN_glAttachShader)(GLuint program, GLuint shader);
typedef void (APIENTRY* PFN_glDetachShader)(GLuint program, GLuint shader);
typedef void (APIENTRY* PFN_glLinkProgram)(GLuint program);
typedef void (APIENTRY* PFN_glGetProgramiv)(GLuint program, GLenum pname, GLint* params);
typedef void (APIENTRY* PFN_glGetProgramInfoLog)(GLuint program, GLsizei bufSize, GLsizei* length, GLchar* infoLog);
typedef void (APIENTRY* PFN_glUseProgram)(GLuint program);
typedef void (APIENTRY* PFN_glDeleteProgram)(GLuint program);
typedef GLint (APIENTRY* PFN_glGetAttribLocation)(GLuint program, const GLchar* name);
typedef GLint (APIENTRY* PFN_glGetUniformLocation)(GLuint program, const GLchar* name);
typedef void (APIENTRY* PFN_glUniform1i)(GLint location, GLint v0);
typedef void (APIENTRY* PFN_glUniformMatrix4fv)(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);
typedef void (APIENTRY* PFN_glUniform4f)(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3);
typedef void (APIENTRY* PFN_glEnableVertexAttribArray)(GLuint index);
typedef void (APIENTRY* PFN_glVertexAttribPointer)(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void* pointer);
typedef void (APIENTRY* PFN_glDisableVertexAttribArray)(GLuint index);
typedef void (APIENTRY* PFN_glGetVertexAttribiv)(GLuint index, GLenum pname, GLint* params);
typedef void (APIENTRY* PFN_glValidateProgram)(GLuint program);
typedef void (APIENTRY* PFN_glBindBuffer)(GLenum target, GLuint buffer);

// Global function pointers
static PFN_glGenFramebuffers ptr_glGenFramebuffers = NULL;
static PFN_glBindFramebuffer ptr_glBindFramebuffer = NULL;
static PFN_glFramebufferTexture2D ptr_glFramebufferTexture2D = NULL;
static PFN_glDeleteFramebuffers ptr_glDeleteFramebuffers = NULL;
static PFN_glCheckFramebufferStatus ptr_glCheckFramebufferStatus = NULL;
static PFN_glActiveTexture ptr_glActiveTexture = NULL;
static PFN_glBlendFuncSeparate ptr_glBlendFuncSeparate = NULL;
static PFN_glCreateShader ptr_glCreateShader = NULL;
static PFN_glShaderSource ptr_glShaderSource = NULL;
static PFN_glCompileShader ptr_glCompileShader = NULL;
static PFN_glGetShaderiv ptr_glGetShaderiv = NULL;
static PFN_glGetShaderInfoLog ptr_glGetShaderInfoLog = NULL;
static PFN_glDeleteShader ptr_glDeleteShader = NULL;
static PFN_glCreateProgram ptr_glCreateProgram = NULL;
static PFN_glAttachShader ptr_glAttachShader = NULL;
static PFN_glDetachShader ptr_glDetachShader = NULL;
static PFN_glLinkProgram ptr_glLinkProgram = NULL;
static PFN_glGetProgramiv ptr_glGetProgramiv = NULL;
static PFN_glGetProgramInfoLog ptr_glGetProgramInfoLog = NULL;
static PFN_glUseProgram ptr_glUseProgram = NULL;
static PFN_glDeleteProgram ptr_glDeleteProgram = NULL;
static PFN_glGetAttribLocation ptr_glGetAttribLocation = NULL;
static PFN_glGetUniformLocation ptr_glGetUniformLocation = NULL;
static PFN_glUniform1i ptr_glUniform1i = NULL;
static PFN_glUniformMatrix4fv ptr_glUniformMatrix4fv = NULL;
static PFN_glUniform4f ptr_glUniform4f = NULL;
static PFN_glEnableVertexAttribArray ptr_glEnableVertexAttribArray = NULL;
static PFN_glVertexAttribPointer ptr_glVertexAttribPointer = NULL;
static PFN_glDisableVertexAttribArray ptr_glDisableVertexAttribArray = NULL;
static PFN_glGetVertexAttribiv ptr_glGetVertexAttribiv = NULL;
static PFN_glValidateProgram ptr_glValidateProgram = NULL;
static PFN_glBindBuffer ptr_glBindBuffer = NULL;

// Wrapper functions
void APIENTRY glGenFramebuffers(GLsizei n, GLuint* framebuffers) { if(ptr_glGenFramebuffers) ptr_glGenFramebuffers(n, framebuffers); }
void APIENTRY glBindFramebuffer(GLenum target, GLuint framebuffer) { if(ptr_glBindFramebuffer) ptr_glBindFramebuffer(target, framebuffer); }
void APIENTRY glFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level) { if(ptr_glFramebufferTexture2D) ptr_glFramebufferTexture2D(target, attachment, textarget, texture, level); }
void APIENTRY glDeleteFramebuffers(GLsizei n, const GLuint* framebuffers) { if(ptr_glDeleteFramebuffers) ptr_glDeleteFramebuffers(n, framebuffers); }
GLenum APIENTRY glCheckFramebufferStatus(GLenum target) { return ptr_glCheckFramebufferStatus ? ptr_glCheckFramebufferStatus(target) : 0; }
void APIENTRY glActiveTexture(GLenum texture) { if(ptr_glActiveTexture) ptr_glActiveTexture(texture); }
void APIENTRY glBlendFuncSeparate(GLenum sfactorRGB, GLenum dfactorRGB, GLenum sfactorAlpha, GLenum dfactorAlpha) { if(ptr_glBlendFuncSeparate) ptr_glBlendFuncSeparate(sfactorRGB, dfactorRGB, sfactorAlpha, dfactorAlpha); }
GLuint APIENTRY glCreateShader(GLenum type) { return ptr_glCreateShader ? ptr_glCreateShader(type) : 0; }
void APIENTRY glShaderSource(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length) { if(ptr_glShaderSource) ptr_glShaderSource(shader, count, string, length); }
void APIENTRY glCompileShader(GLuint shader) { if(ptr_glCompileShader) ptr_glCompileShader(shader); }
void APIENTRY glGetShaderiv(GLuint shader, GLenum pname, GLint* params) { if(ptr_glGetShaderiv) ptr_glGetShaderiv(shader, pname, params); }
void APIENTRY glGetShaderInfoLog(GLuint shader, GLsizei bufSize, GLsizei* length, GLchar* infoLog) { if(ptr_glGetShaderInfoLog) ptr_glGetShaderInfoLog(shader, bufSize, length, infoLog); }
void APIENTRY glDeleteShader(GLuint shader) { if(ptr_glDeleteShader) ptr_glDeleteShader(shader); }
GLuint APIENTRY glCreateProgram(void) { return ptr_glCreateProgram ? ptr_glCreateProgram() : 0; }
void APIENTRY glAttachShader(GLuint program, GLuint shader) { if(ptr_glAttachShader) ptr_glAttachShader(program, shader); }
void APIENTRY glDetachShader(GLuint program, GLuint shader) { if(ptr_glDetachShader) ptr_glDetachShader(program, shader); }
void APIENTRY glLinkProgram(GLuint program) { if(ptr_glLinkProgram) ptr_glLinkProgram(program); }
void APIENTRY glGetProgramiv(GLuint program, GLenum pname, GLint* params) { if(ptr_glGetProgramiv) ptr_glGetProgramiv(program, pname, params); }
void APIENTRY glGetProgramInfoLog(GLuint program, GLsizei bufSize, GLsizei* length, GLchar* infoLog) { if(ptr_glGetProgramInfoLog) ptr_glGetProgramInfoLog(program, bufSize, length, infoLog); }
void APIENTRY glUseProgram(GLuint program) { if(ptr_glUseProgram) ptr_glUseProgram(program); }
void APIENTRY glDeleteProgram(GLuint program) { if(ptr_glDeleteProgram) ptr_glDeleteProgram(program); }
GLint APIENTRY glGetAttribLocation(GLuint program, const GLchar* name) { return ptr_glGetAttribLocation ? ptr_glGetAttribLocation(program, name) : -1; }
GLint APIENTRY glGetUniformLocation(GLuint program, const GLchar* name) { return ptr_glGetUniformLocation ? ptr_glGetUniformLocation(program, name) : -1; }
void APIENTRY glUniform1i(GLint location, GLint v0) { if(ptr_glUniform1i) ptr_glUniform1i(location, v0); }
void APIENTRY glUniformMatrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value) { if(ptr_glUniformMatrix4fv) ptr_glUniformMatrix4fv(location, count, transpose, value); }
void APIENTRY glUniform4f(GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3) { if(ptr_glUniform4f) ptr_glUniform4f(location, v0, v1, v2, v3); }
void APIENTRY glEnableVertexAttribArray(GLuint index) { if(ptr_glEnableVertexAttribArray) ptr_glEnableVertexAttribArray(index); }
void APIENTRY glVertexAttribPointer(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void* pointer) { if(ptr_glVertexAttribPointer) ptr_glVertexAttribPointer(index, size, type, normalized, stride, pointer); }
void APIENTRY glDisableVertexAttribArray(GLuint index) { if(ptr_glDisableVertexAttribArray) ptr_glDisableVertexAttribArray(index); }
void APIENTRY glGetVertexAttribiv(GLuint index, GLenum pname, GLint* params) { if(ptr_glGetVertexAttribiv) ptr_glGetVertexAttribiv(index, pname, params); }
void APIENTRY glValidateProgram(GLuint program) { if(ptr_glValidateProgram) ptr_glValidateProgram(program); }
void APIENTRY glBindBuffer(GLenum target, GLuint buffer) { if(ptr_glBindBuffer) ptr_glBindBuffer(target, buffer); }

void init_gles2_shim() {
    static int initialized = 0;
    if (initialized) return;
    HMODULE hLib = GetModuleHandleA("opengl32.dll");
    if (!hLib) {
        fprintf(stderr, "Failed to get handle for opengl32.dll\n");
        return;
    }

    #define LOAD(ptr, name, type) \
        ptr = (type)wglGetProcAddress(#name); \
        if (!ptr) ptr = (type)GetProcAddress(hLib, #name); \
        if (!ptr) fprintf(stderr, "Failed to load function: %s\n", #name)

    #define LOAD_EXT(ptr, name, type, ext) \
        ptr = (type)wglGetProcAddress(#name); \
        if (!ptr) ptr = (type)wglGetProcAddress(#name #ext); \
        if (!ptr) ptr = (type)GetProcAddress(hLib, #name); \
        if (!ptr) fprintf(stderr, "Failed to load function: %s (and %s)\n", #name, #name #ext)

    LOAD_EXT(ptr_glGenFramebuffers, glGenFramebuffers, PFN_glGenFramebuffers, EXT);
    LOAD_EXT(ptr_glBindFramebuffer, glBindFramebuffer, PFN_glBindFramebuffer, EXT);
    LOAD_EXT(ptr_glFramebufferTexture2D, glFramebufferTexture2D, PFN_glFramebufferTexture2D, EXT);
    LOAD_EXT(ptr_glDeleteFramebuffers, glDeleteFramebuffers, PFN_glDeleteFramebuffers, EXT);
    LOAD_EXT(ptr_glCheckFramebufferStatus, glCheckFramebufferStatus, PFN_glCheckFramebufferStatus, EXT);
    LOAD_EXT(ptr_glActiveTexture, glActiveTexture, PFN_glActiveTexture, ARB);
    LOAD(ptr_glBlendFuncSeparate, glBlendFuncSeparate, PFN_glBlendFuncSeparate);
    LOAD(ptr_glCreateShader, glCreateShader, PFN_glCreateShader);
    LOAD(ptr_glShaderSource, glShaderSource, PFN_glShaderSource);
    LOAD(ptr_glCompileShader, glCompileShader, PFN_glCompileShader);
    LOAD(ptr_glGetShaderiv, glGetShaderiv, PFN_glGetShaderiv);
    LOAD(ptr_glGetShaderInfoLog, glGetShaderInfoLog, PFN_glGetShaderInfoLog);
    LOAD(ptr_glDeleteShader, glDeleteShader, PFN_glDeleteShader);
    LOAD(ptr_glCreateProgram, glCreateProgram, PFN_glCreateProgram);
    LOAD(ptr_glAttachShader, glAttachShader, PFN_glAttachShader);
    LOAD(ptr_glDetachShader, glDetachShader, PFN_glDetachShader);
    LOAD(ptr_glLinkProgram, glLinkProgram, PFN_glLinkProgram);
    LOAD(ptr_glGetProgramiv, glGetProgramiv, PFN_glGetProgramiv);
    LOAD(ptr_glGetProgramInfoLog, glGetProgramInfoLog, PFN_glGetProgramInfoLog);
    LOAD(ptr_glUseProgram, glUseProgram, PFN_glUseProgram);
    LOAD(ptr_glDeleteProgram, glDeleteProgram, PFN_glDeleteProgram);
    LOAD(ptr_glGetAttribLocation, glGetAttribLocation, PFN_glGetAttribLocation);
    LOAD(ptr_glGetUniformLocation, glGetUniformLocation, PFN_glGetUniformLocation);
    LOAD(ptr_glUniform1i, glUniform1i, PFN_glUniform1i);
    LOAD(ptr_glUniformMatrix4fv, glUniformMatrix4fv, PFN_glUniformMatrix4fv);
    LOAD(ptr_glUniform4f, glUniform4f, PFN_glUniform4f);
    LOAD(ptr_glEnableVertexAttribArray, glEnableVertexAttribArray, PFN_glEnableVertexAttribArray);
    LOAD(ptr_glVertexAttribPointer, glVertexAttribPointer, PFN_glVertexAttribPointer);
    LOAD(ptr_glDisableVertexAttribArray, glDisableVertexAttribArray, PFN_glDisableVertexAttribArray);
    LOAD(ptr_glGetVertexAttribiv, glGetVertexAttribiv, PFN_glGetVertexAttribiv);
    LOAD(ptr_glValidateProgram, glValidateProgram, PFN_glValidateProgram);
    LOAD(ptr_glBindBuffer, glBindBuffer, PFN_glBindBuffer);
    
    initialized = 1;
}
#endif
