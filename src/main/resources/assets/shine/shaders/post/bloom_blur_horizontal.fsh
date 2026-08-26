#version 330

uniform sampler2D InSampler;
uniform vec2 BlurDir;
uniform float Radius;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / OutSize;
    vec2 texel = BlurDir / InSize;
    float actualRadius = min(max(Radius, 0.0), 64.0);
    float sampleCount = 6.0;
    vec3 color = texture(InSampler, uv).rgb * 0.227027;
    for (int i = 1; i <= 6; ++i) {
        float offset = actualRadius * (float(i) / sampleCount);
        float weight = 0.18 / sampleCount;
        color += texture(InSampler, uv + texel * offset).rgb * weight;
        color += texture(InSampler, uv - texel * offset).rgb * weight;
    }
    fragColor = vec4(color, 1.0);
}
