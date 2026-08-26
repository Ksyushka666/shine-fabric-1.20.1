#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MaskSampler;
uniform sampler2D HalfSampler;
uniform sampler2D QuarterSampler;
uniform sampler2D EighthSampler;
uniform sampler2D SixteenthSampler;
uniform sampler2D ThirtysecondSampler;
uniform sampler2D SixtyfourthSampler;

uniform vec2 OutSize;
uniform vec2 MainSize;
uniform vec2 HalfSize;
uniform vec2 QuarterSize;
uniform vec2 EighthSize;
uniform vec2 SixteenthSize;
uniform vec2 ThirtysecondSize;
uniform vec2 SixtyfourthSize;

uniform float Strength;
uniform float SelectiveMask;

uniform float Weight0;
uniform float Weight1;
uniform float Weight2;
uniform float Weight3;
uniform float Weight4;
uniform float Weight5;

uniform float MaxDistance;
uniform float NearPlane;
uniform float FarPlane;
uniform float DistanceFadeRange;

out vec4 fragColor;

vec3 centered_sample(sampler2D sampler, vec2 uv, vec2 levelSize) {
    vec2 halfTexelX = vec2(0.5 / max(levelSize.x, 1.0), 0.0);
    return (texture(sampler, uv - halfTexelX).rgb + texture(sampler, uv + halfTexelX).rgb) * 0.5;
}

void main() {
    vec2 uv = gl_FragCoord.xy / OutSize;
    vec4 sceneColor = texture(DiffuseSampler, uv);
    float distanceMask = 1.0;
    vec3 bloomColor = centered_sample(HalfSampler, uv, HalfSize) * Weight0;
    bloomColor += centered_sample(QuarterSampler, uv, QuarterSize) * Weight1;
    bloomColor += centered_sample(EighthSampler, uv, EighthSize) * Weight2;
    bloomColor += centered_sample(SixteenthSampler, uv, SixteenthSize) * Weight3;
    bloomColor += centered_sample(ThirtysecondSampler, uv, ThirtysecondSize) * Weight4;
    bloomColor += centered_sample(SixtyfourthSampler, uv, SixtyfourthSize) * Weight5;
    float selectiveMask = SelectiveMask > 0.5 ? clamp(texture(MaskSampler, uv).r, 0.0, 1.0) : 1.0;
    bloomColor *= Strength * distanceMask * selectiveMask;
    fragColor = vec4(sceneColor.rgb + bloomColor, sceneColor.a);
}
