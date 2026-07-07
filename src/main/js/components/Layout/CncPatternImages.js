import React, { Component } from 'react';

// Reference images shown in the pattern detail of /cncPs and /cncControl.
// The file is named by characters 11-13 (1-based) of each code, any extension
// (e.g. PN Cuir L003206673CXNAF -> CXN.png). Served from /api/public/cncImages
// (permitAll) so a plain <img> tag loads without the JWT Authorization header.

const code11to13 = (value) => {
    const v = (value || '').trim();
    return v.length >= 13 ? v.substring(10, 13) : null;
};

const imgUrl = (kind, value) =>
    `/api/public/cncImages?kind=${kind}&value=${encodeURIComponent((value || '').trim())}`;

class CncImage extends Component {
    state = { error: false };

    render() {
        const { kind, value, label } = this.props;
        const code = code11to13(value);
        return (
            <div className="text-center mr-4 mb-2">
                <div className="small font-weight-bold mb-1">
                    {label}{code ? ` (${code})` : ''}
                </div>
                {!code ? (
                    <div className="text-muted small">— code &lt; 13 car.</div>
                ) : this.state.error ? (
                    <div className="text-muted small">Image {code} introuvable</div>
                ) : (
                    <img
                        src={imgUrl(kind, value)}
                        alt={code}
                        style={{ maxHeight: 170, maxWidth: 260, border: '1px solid #ddd', borderRadius: 4 }}
                        onError={() => this.setState({ error: true })}
                    />
                )}
            </div>
        );
    }
}

// leatherPn = box leather (session.code1Imp); filCouture = pattern's coutureDecorativeCnc.
const CncPatternImages = ({ leatherPn, filCouture }) => (
    <div className="d-flex flex-wrap align-items-start p-2">
        <CncImage kind="leather" value={leatherPn} label="PN Cuir" />
        <CncImage kind="filCouture" value={filCouture} label="Fil Couture CNC" />
    </div>
);

export default CncPatternImages;
