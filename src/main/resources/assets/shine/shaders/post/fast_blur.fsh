#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D InSampler;

uniform vec2 OutSize;
uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Radius;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    if (Radius < 0.5 || dot(BlurDir, BlurDir) < 1e-6) {
        fragColor = texture(InSampler, texCoord);
        return;
    }

    vec2 oneTexel = 1.0 / InSize;
    vec2 sampleStep = oneTexel * BlurDir;
    float actualRadius = max(0.5, Radius);
    // Keep the configured effective radius while bounding the shader cost to 13 fetches.
    // This is important for Shine's original radius range (up to 700) on 1.20.1 hardware.
    const float sampleCount = 6.0;
    vec4 blurred = texture(InSampler, texCoord);
    for (float i = 1.0; i <= sampleCount; i += 1.0) {
        float offset = actualRadius * (i / sampleCount);
        blurred += texture(InSampler, texCoord + sampleStep * offset);
        blurred += texture(InSampler, texCoord - sampleStep * offset);
    }
    fragColor = blurred / (sampleCount * 2.0 + 1.0);
}
